package io.github.kylstevenson.unparser;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.Properties;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;

class UnparserPluginBootstrapTest {

  @Test
  void pluginDescriptorPointsAtJavaPluginMainClass() throws Exception {
    Properties descriptor = new Properties();
    try (InputStream stream = getClass().getClassLoader().getResourceAsStream("plugin.yml")) {
      assertNotNull(stream);
      descriptor.load(stream);
    }

    String mainClassName = descriptor.getProperty("main");
    assertNotNull(mainClassName);

    Class<?> mainClass = Class.forName(mainClassName);
    assertTrue(JavaPlugin.class.isAssignableFrom(mainClass));
  }
}
