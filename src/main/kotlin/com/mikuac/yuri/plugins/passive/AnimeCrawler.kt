package com.mikuac.yuri.plugins.passive

import com.google.gson.Gson
import com.mikuac.shiro.annotation.AnyMessageHandler
import com.mikuac.shiro.annotation.common.Shiro
import com.mikuac.shiro.common.utils.MsgUtils
import com.mikuac.shiro.core.Bot
import com.mikuac.shiro.dto.event.message.AnyMessageEvent
import com.mikuac.yuri.annotation.Slf4j
import com.mikuac.yuri.dto.AnimeCrawlerDTO
import com.mikuac.yuri.enums.Regex
import com.mikuac.yuri.exception.ExceptionHandler
import com.mikuac.yuri.exception.YuriException
import com.mikuac.yuri.utils.NetUtils
import org.springframework.stereotype.Component
import java.awt.Color
import java.awt.Font
import java.awt.Image
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.net.URL
import java.util.*
import java.util.regex.Matcher
import javax.imageio.ImageIO

@Slf4j
@Shiro
@Component
class AnimeCrawler {

    private val font = Font.createFont(Font.TRUETYPE_FONT, this.javaClass.getResourceAsStream("/font/chinese_font.ttf"))

    private fun request(): AnimeCrawlerDTO {
        return NetUtils.get("https://bangumi.bilibili.com/web_api/timeline_global").use { resp ->
            val data = Gson().fromJson(resp.body?.string(), AnimeCrawlerDTO::class.java)
            if (data.code != 0) throw YuriException(data.message)
            data
        }
    }

    private fun getAnimeForDate(dayOfWeek: Int): String {
        request().result.forEach { result ->
            if (result.dayOfWeek != dayOfWeek) {
                return@forEach
            }
            return "base64://${drawImage(result.seasons)}"
        }
        return "今日暂无番剧放送"
    }

    private fun getTodayAnime(): String {
        request().result.forEach { result ->
            if (result.isToday != 1) {
                return@forEach
            }
            return "base64://${drawImage(result.seasons)}"
        }
        return "今日暂无番剧放送"
    }

    private fun drawImage(seasons: List<AnimeCrawlerDTO.Result.Season>): String {
        val hBorderWidth = 18
        val previewHeight = 600
        val oneAnimeHeight = previewHeight + 2 * hBorderWidth
        val totalImgHeight = seasons.size * oneAnimeHeight
        val totalImageWidth = 1200

        val bufferedImage = BufferedImage(totalImageWidth, totalImgHeight, BufferedImage.TYPE_INT_RGB)
        var graphics = bufferedImage.createGraphics()
        graphics.color = Color.decode("#FFFFFF")
        graphics.fillRect(0, 0, bufferedImage.width, bufferedImage.height)
        graphics.dispose()
        val titleFont = font.deriveFont(40f).deriveFont(Font.BOLD)

        seasons.forEachIndexed { index, season ->
            // Skip delayed episodes
            if (season.delay != 0) {
                return@forEachIndexed
            }
            val curHeight = index * oneAnimeHeight
            val imgY = curHeight + hBorderWidth
            val oneAnimeImg = ImageIO.read(URL(season.cover))
            val previewWidth = oneAnimeImg.width * previewHeight / oneAnimeImg.height
            val oneAnimeImgScaled = oneAnimeImg.getScaledInstance(previewWidth, previewHeight, Image.SCALE_SMOOTH)
            val imgX = if (index % 2 == 0) {
                20
            } else {
                totalImageWidth - 20 - previewWidth
            }
            val vBorderWidth = if (index % 2 == 0) {
                imgX
            } else {
                totalImageWidth - imgX - previewWidth
            }
            // borders
            graphics = bufferedImage.createGraphics()
            graphics.color = Color.decode("#FFC0CB")
            graphics.fillRect(0, curHeight, totalImageWidth, hBorderWidth)
            graphics.fillRect(0, curHeight, vBorderWidth, oneAnimeHeight)
            graphics.fillRect(0, imgY + previewHeight, totalImageWidth, hBorderWidth)
            graphics.fillRect(totalImageWidth - vBorderWidth, curHeight, vBorderWidth, oneAnimeHeight)
            graphics.dispose()
            // preview image
            graphics = bufferedImage.createGraphics()
            graphics.drawImage(oneAnimeImgScaled, imgX, imgY, null)
            graphics.dispose()
            var titleSize = 65f
            val titleTextWidth = 750f
            while (titleSize * season.title.length > titleTextWidth) {
                titleSize -= 1
            }
            // title text
            val titleY = imgY - 30 + (previewHeight / 2)
            val titleX = if (index % 2 == 0) {
                870 - (season.title.length / 2 * titleSize).toInt()
            } else {
                420 - (season.title.length / 2 * titleSize).toInt()
            }
            graphics = bufferedImage.createGraphics()
            graphics.font = titleFont.deriveFont(titleSize)
            graphics.color = Color.decode("#4E574C")
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            graphics.drawString(season.title.trim(), titleX, titleY)
            graphics.dispose()
            // detailed info
            val timeY = titleY + 60
            val timeX = titleX + (season.title.length / 2 * titleSize).toInt() - 80
            graphics = bufferedImage.createGraphics()
            graphics.font = font.deriveFont(35f)
            graphics.color = Color.decode("#798777")
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            graphics.drawString(season.pubTime.trim(), timeX, timeY)
            graphics.dispose()
        }

        val stream = ByteArrayOutputStream()
        ImageIO.write(bufferedImage, "PNG", stream)
        return Base64.getEncoder().encodeToString(stream.toByteArray())
    }

    private fun buildMsg(matcher: Matcher): String {
        if (matcher.group(1) != null) {
            return getTodayAnime()
        }
        return when (matcher.group(3)) {
            in listOf("1", "一") -> getAnimeForDate(1)
            in listOf("2", "二") -> getAnimeForDate(2)
            in listOf("3", "三") -> getAnimeForDate(3)
            in listOf("4", "四") -> getAnimeForDate(4)
            in listOf("5", "五") -> getAnimeForDate(5)
            in listOf("6", "六") -> getAnimeForDate(6)
            in listOf("7", "七", "日", "天") -> getAnimeForDate(7)
            else -> "请输入正确的星期"
        }
    }

    @AnyMessageHandler(cmd = Regex.ANIME_CRAWLER)
    fun handler(bot: Bot, event: AnyMessageEvent, matcher: Matcher) {
        ExceptionHandler.with(bot, event) {
            var msg: String = buildMsg(matcher)
            if (msg.startsWith("base64://")) msg = MsgUtils.builder().img(msg).build()
            bot.sendMsg(event, msg, false)
        }
    }

}