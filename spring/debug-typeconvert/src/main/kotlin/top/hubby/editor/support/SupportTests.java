package top.hubby.editor.support;

import lombok.extern.slf4j.Slf4j;

import org.junit.Test;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.beans.PropertyEditorRegistrySupport;
import org.springframework.beans.propertyeditors.UUIDEditor;

import top.hubby.model.Animal;
import top.hubby.model.Cat;
import top.hubby.model.Person;

import java.beans.PropertyEditor;
import java.util.UUID;

/**
 * @author asd <br>
 * @create 2022-01-19 4:36 PM <br>
 * @project boot-typeconvert <br>
 */
@Slf4j
public class SupportTests {
    @Test
    public void testAnimalPropertyEditor() {
        PropertyEditorRegistry propertyEditorRegistry = new PropertyEditorRegistrySupport();
        propertyEditorRegistry.registerCustomEditor(Animal.class, new AnimalPropertyEditor());

        // 父类型、子类型均可匹配上对应的编辑器
        PropertyEditor customEditor1 = propertyEditorRegistry.findCustomEditor(Cat.class, null);
        PropertyEditor customEditor2 = propertyEditorRegistry.findCustomEditor(Animal.class, null);
        assert Animal.class.isAssignableFrom(Cat.class);
        log.info(String.valueOf(customEditor1 == customEditor2));
        log.info(customEditor1.getClass().getSimpleName());
        assert customEditor2.getAsText() == null;
    }

    @Test
    public void testPersonCatUUIDEditor() {
        PropertyEditorRegistry propertyEditorRegistry = new PropertyEditorRegistrySupport();
        propertyEditorRegistry.registerCustomEditor(UUID.class, new UUIDEditor());
        propertyEditorRegistry.registerCustomEditor(
                Person.class, "cat.uuid", new PersonCatUUIDEditor());

        String uuidStr = "1-2-3-4-5";
        String personCatUuidStr = "1-2-3-4-5_YourBatman";
        // String personCatUuidStr = "1-2-3-4-5_YourBatman";

        PropertyEditor customEditor = propertyEditorRegistry.findCustomEditor(UUID.class, null);
        // 抛异常：java.lang.NumberFormatException: For input string: "5_YourBatman"
        // customEditor.setAsText(personCatUuidStr);

        customEditor.setAsText(uuidStr);
        log.info(customEditor.getAsText());

        customEditor = propertyEditorRegistry.findCustomEditor(Person.class, "cat.uuid");
        customEditor.setAsText(personCatUuidStr);
        log.info(customEditor.getAsText());
    }
}
