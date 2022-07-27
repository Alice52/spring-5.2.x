import org.springframework.web.reactive.result.view.script.foo
import org.springframework.web.reactive.result.view.script.i18n
import org.springframework.web.reactive.result.view.script.include

"""${include("header")}
<p>${i18n("hello")} $foo</p>
${include("footer")}"""
