package com.mikuac.yuri.plugins.passive

import com.mikuac.shiro.annotation.AnyMessageHandler
import com.mikuac.shiro.annotation.common.Shiro
import com.mikuac.shiro.core.Bot
import com.mikuac.shiro.dto.event.message.AnyMessageEvent
import com.mikuac.yuri.config.Config
import com.mikuac.yuri.enums.RegexCMD
import com.mikuac.yuri.exception.YuriException
import com.mikuac.yuri.utils.SearchModeUtils
import com.mikuac.yuri.utils.SendUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Shiro
@Component
class PicSearch {

    @Autowired
    private lateinit var sauceNao: SauceNao

    @Autowired
    private lateinit var ascii2d: Ascii2d

    private val cfg = Config.plugins.picSearch

    @AnyMessageHandler(cmd = RegexCMD.SAUCE_NAO_SEARCH)
    fun picHandler(bot: Bot, event: AnyMessageEvent) {
        SearchModeUtils.setSearchMode(this.javaClass.simpleName, event.userId, event.groupId ?: 0L, bot)
    }

    @AnyMessageHandler
    fun picSearch(bot: Bot, event: AnyMessageEvent) {
        if (!SearchModeUtils.check(this.javaClass.simpleName, event.userId, event.groupId ?: 0L)) return
        // 发送检索结果
        try {
            val imgUrl = SearchModeUtils.getImgUrl(event.userId, event.groupId, event.arrayMsg)
            if (imgUrl.isNullOrBlank()) return
            val imgMd5 = imgUrl.split("-").last()
            // SauceNao
            val sauceNaoResult = sauceNao.buildMsgForSauceNao(imgUrl, imgMd5)
            bot.sendMsg(event, sauceNaoResult.second, false)

            if (!cfg.alwaysUseAscii2d) {
                val similarity = sauceNaoResult.first
                if (similarity.isNotBlank() && similarity.toFloat() > cfg.similarity.toFloat()) return
            }

            bot.sendMsg(event, "检索结果相似度较低，正在使用Ascii2d进行检索···", false)
            val ascii2dResult = ascii2d.buildMsgForAscii2d(imgUrl, imgMd5)
            // Ascii2d 色合検索
            bot.sendMsg(event, ascii2dResult.first, false)
            // Ascii2d 特徴検索
            bot.sendMsg(event, ascii2dResult.second, false)
        } catch (e: YuriException) {
            e.message?.let { SendUtils.reply(event, bot, it) }
        } catch (e: Exception) {
            SendUtils.reply(event, bot, "未知错误：${e.message}")
            e.printStackTrace()
        }
    }

}