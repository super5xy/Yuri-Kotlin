package com.mikuac.yuri.plugins.passive

import com.google.gson.Gson
import com.mikuac.shiro.annotation.AnyMessageHandler
import com.mikuac.shiro.annotation.common.Shiro
import com.mikuac.shiro.common.utils.MsgUtils
import com.mikuac.shiro.core.Bot
import com.mikuac.shiro.dto.event.message.AnyMessageEvent
import com.mikuac.yuri.annotation.Slf4j
import com.mikuac.yuri.config.Config
import com.mikuac.yuri.dto.AnimePicDTO
import com.mikuac.yuri.enums.Regex
import com.mikuac.yuri.exception.ExceptionHandler
import com.mikuac.yuri.exception.YuriException
import com.mikuac.yuri.utils.ImageUtils
import com.mikuac.yuri.utils.NetUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.jodah.expiringmap.ExpirationPolicy
import net.jodah.expiringmap.ExpiringMap
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit
import java.util.regex.Matcher

@Slf4j
@Shiro
@Component
class SeTu {

    private val cfg = Config.plugins.setu

    private val expiringMap: ExpiringMap<Long, Long> = ExpiringMap.builder()
        .variableExpiration()
        .expirationPolicy(ExpirationPolicy.CREATED)
        .expiration(cfg.cd.times(1000L), TimeUnit.MILLISECONDS)
        .build()

    private fun request(r18: Boolean): AnimePicDTO.Data {
        return NetUtils.get("https://api.lolicon.app/setu/v2${if (r18) "?r18=1" else ""}").use { resp ->
            val data = Gson().fromJson(resp.body?.string(), AnimePicDTO::class.java)
            if (data.error.isNotEmpty()) throw YuriException(data.error)
            if (data.data.isEmpty()) throw YuriException("列表为空")
            data.data[0]
        }
    }

    private fun buildTextMsg(r18: Boolean): Pair<String, String> {
        val data = request(r18)
        val imgUrl = data.urls.original
        return Pair(
            MsgUtils.builder()
                .text("标题：${data.title}")
                .text("\nPID：${data.pid}")
                .text("\n作者：${data.author}")
                .text("\n链接：https://www.pixiv.net/artworks/${data.pid}")
                .text("\n反代链接：${imgUrl}")
                .build(),
            imgUrl
        )
    }

    private fun buildPicMsg(url: String): String {
        return MsgUtils.builder().img(ImageUtils.imgAntiShielding(url, cfg.antiShielding, cfg.proxy)).build()
    }

    private fun recallMsgPic(msgId: Int, bot: Bot) = runBlocking {
        launch {
            delay(cfg.recallPicTime.times(1000L))
            bot.deleteMsg(msgId)
        }
    }

    private fun buildMsg(r18: Boolean, userId: Long, groupId: Long): Pair<String, String> {
        // 检查是否处于冷却时间
        if (expiringMap[groupId + userId] != null && expiringMap[groupId + userId] == userId) {
            val expectedExpiration = expiringMap.getExpectedExpiration(groupId + userId) / 1000
            throw YuriException("整天色图色图，信不信把你变成色图？冷却：[${expectedExpiration}秒]")
        }
        if (!cfg.r18 && r18) throw YuriException("NSFW禁止！")
        val buildTextMsg = buildTextMsg(r18)
        return Pair(buildTextMsg.first, buildTextMsg.second)
    }

    @AnyMessageHandler(cmd = Regex.SETU)
    fun handler(bot: Bot, event: AnyMessageEvent, matcher: Matcher) {
        ExceptionHandler.with(bot, event) {
            val groupId = event.groupId ?: 0L
            val r18 = matcher.group("r18")
            val msg = buildMsg(!r18.isNullOrBlank(), event.userId, groupId)
            bot.sendMsg(event, msg.first, false)
            val cdTime = cfg.cd.times(1000L)
            expiringMap.put(groupId + event.userId, event.userId, cdTime, TimeUnit.MILLISECONDS)
            val picMsgId = bot.sendMsg(event, buildPicMsg(msg.second), false)?.data?.messageId
            picMsgId?.let { recallMsgPic(it, bot) }
        }
    }

}