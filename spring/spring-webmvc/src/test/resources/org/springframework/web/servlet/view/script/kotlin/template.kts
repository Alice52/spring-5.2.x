import org.springframework.web.servlet.view.script.foo
import org.springframework.web.servlet.view.script.i18n
import org.springframework.web.servlet.view.script.include

"""${include("header")}
<p>${i18n("hello")} $foo</p>
${include("footer")}"""
