package com.mikuac.yuri.plugins.initiative

import cn.hutool.core.util.IdUtil
import com.mikuac.shiro.common.utils.MsgUtils
import com.mikuac.yuri.config.Config
import com.mikuac.yuri.config.ConfigModel.Plugins.Telegram.Rules.RuleItem
import com.mikuac.yuri.global.Global
import com.mikuac.yuri.utils.BeanUtils
import com.mikuac.yuri.utils.FFmpegUtils
import com.mikuac.yuri.utils.ImageUtils.formatPNG
import com.mikuac.yuri.utils.NetUtils
import com.mikuac.yuri.utils.TelegramUtils.getFile
import lombok.extern.slf4j.Slf4j
import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.objects.PhotoSize
import org.telegram.telegrambots.meta.api.objects.Update
import java.util.function.Supplier
import java.util.stream.Stream

@Slf4j
class TelegramForward(opts: DefaultBotOptions, token: String) : TelegramLongPollingBot(opts, token) {

    private companion object {
        const val GROUP = "group"
        const val PRIVATE = "private"
        const val CHANNEL = "channel"
        const val SUPER_GROUP = "supergroup"
    }

    private val cfg = Config.plugins.telegram

    override fun getBotUsername(): String {
        return cfg.botUsername
    }

    @Suppress("kotlin:S3776")
    override fun onUpdateReceived(update: Update) {
        // check enable and has message
        if (!cfg.enable || !update.hasMessage()) {
            return
        }

        val message = update.message
        val chat = message.chat

        val msg = MsgUtils.builder()
        if (message.hasText()) {
            msg.text(message.text)
        }

        if (message.hasPhoto()) {
            message.photo.stream().max(Comparator.comparingInt { obj: PhotoSize -> obj.fileSize }).ifPresent {
                getFile(it.fileId, cfg.proxy).let { url ->
                    if (url.isNotBlank()) msg.img(formatPNG(url, cfg.proxy))
                }
            }
            val caption = message.caption
            if (caption != null && caption.isNotBlank()) {
                msg.text("\n")
                msg.text(message.caption)
            }
        }

        if (message.hasSticker() && !message.sticker.isAnimated && !message.sticker.isVideo) {
            getFile(message.sticker.fileId, cfg.proxy).let { url ->
                if (url.isNotBlank()) msg.img(formatPNG(url, cfg.proxy))
            }
        }

        @Suppress("kotlin:S125")
        // if (message.hasSticker() && message.sticker.isAnimated) {
        //     val fileId = message.sticker.fileId
        //     getFile(fileId).takeIf { it.isNotBlank() }?.let { url ->
        //         println(url)
        //     }
        // }

        if (message.hasSticker() && message.sticker.isVideo) {
            val fileId = message.sticker.fileId
            getFile(fileId, cfg.proxy).takeIf { it.isNotBlank() }?.let { url ->
                if (url.endsWith(".webm")) {
                    NetUtils.download(
                        getFile(fileId, cfg.proxy),
                        "cache/telegram",
                        "${IdUtil.simpleUUID()}.webm",
                        cfg.proxy
                    ).let {
                        msg.img("file://${FFmpegUtils.webm2Gif(it)}")
                    }
                }
            }
        }

        val fromUser = message.from.userName
        if (msg.build().isNotBlank()) {
            msg.text("\n发送者：$fromUser")
            when (chat.type) {
                PRIVATE -> msg.text("\nTG私聊：$fromUser")
                CHANNEL -> msg.text("\nTG频道：${chat.title}")
                GROUP, SUPER_GROUP -> msg.text("\nTG群组：${chat.title}")
            }
        }

        // check user white list
        if (listOf(GROUP, SUPER_GROUP).contains(chat.type) && cfg.enableUserWhiteList && !cfg.userWhiteList.contains(
                fromUser
            )
        ) return

        send(chat.type, msg.build(), fromUser ?: "", chat.title ?: "")
    }

    private fun send(type: String, msg: String, fromUser: String, title: String) {
        when (type) {
            PRIVATE -> {
                val target = Supplier<Stream<RuleItem>> { cfg.rules.friend.stream().filter { it.source == fromUser } }
                handler(target, msg)
            }

            CHANNEL -> {
                val target = Supplier<Stream<RuleItem>> { cfg.rules.channel.stream().filter { it.source == title } }
                handler(target, msg)
            }

            GROUP, SUPER_GROUP -> {
                val target = Supplier<Stream<RuleItem>> { cfg.rules.group.stream().filter { it.source == title } }
                handler(target, msg)
            }
        }
    }

    private fun handler(targets: Supplier<Stream<RuleItem>>, msg: String) {
        val bot = BeanUtils.getBean(Global::class.java).bot()
        targets.get().forEach { t ->
            t.target.friend.forEach {
                bot.sendPrivateMsg(it, msg, false)
            }
        }

        targets.get().forEach { t ->
            t.target.group.forEach {
                bot.sendGroupMsg(it, msg, false)
            }

        }
    }

}