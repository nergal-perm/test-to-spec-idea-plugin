package ewc.openspec.testlink

import com.intellij.ui.ColorUtil
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer

object MarkdownRenderer {

    private val parser: Parser = Parser.builder().build()
    private val renderer: HtmlRenderer = HtmlRenderer.builder().build()

    fun renderToHtml(markdown: String): String {
        val document = parser.parse(markdown)
        return renderer.render(document)
    }

    fun wrapInHtml(bodyHtml: String): String {
        val bg = ColorUtil.toHex(UIUtil.getToolTipBackground())
        val fg = ColorUtil.toHex(UIUtil.getToolTipForeground())
        val linkColor = ColorUtil.toHex(JBUI.CurrentTheme.Link.Foreground.ENABLED)
        val font = UIUtil.getLabelFont()
        val fontFamily = font.family
        val fontSize = font.size

        val codeBg = if (ColorUtil.isDark(UIUtil.getToolTipBackground())) "333333" else "f0f0f0"
        val borderColor = if (ColorUtil.isDark(UIUtil.getToolTipBackground())) "555555" else "cccccc"

        return """
            <html>
            <head>
            <style>
                body {
                    font-family: '$fontFamily', sans-serif;
                    font-size: ${fontSize}pt;
                    color: #$fg;
                    background-color: #$bg;
                    margin: 8px 12px;
                }
                h1, h2, h3, h4, h5, h6 {
                    margin-top: 8px;
                    margin-bottom: 4px;
                    color: #$fg;
                }
                p { margin: 4px 0; }
                code {
                    font-family: 'JetBrains Mono', monospace;
                    font-size: ${fontSize - 1}pt;
                    background-color: #$codeBg;
                    padding: 1px 4px;
                }
                pre {
                    background-color: #$codeBg;
                    border: 1px solid #$borderColor;
                    padding: 8px;
                }
                pre code {
                    background-color: transparent;
                    padding: 0;
                }
                ul, ol { padding-left: 20px; margin: 4px 0; }
                li { margin: 2px 0; }
                a { color: #$linkColor; }
                hr {
                    border-top: 1px solid #$borderColor;
                    margin: 8px 0;
                }
                table { margin: 4px 0; }
                th, td {
                    border: 1px solid #$borderColor;
                    padding: 4px 8px;
                    text-align: left;
                }
                blockquote {
                    border-left: 3px solid #$borderColor;
                    margin: 4px 0;
                    padding-left: 12px;
                    color: #$fg;
                }
                strong { font-weight: bold; }
                em { font-style: italic; }
            </style>
            </head>
            <body>
            $bodyHtml
            </body>
            </html>
        """.trimIndent()
    }
}
