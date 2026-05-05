package io.github.kylstevenson.unparser.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.kylstevenson.unparser.UnparserManager;
import io.github.kylstevenson.unparser.command.framework.CommandArgs;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;

class UnparseCommandTest {

  @Test
  void helpModeSendsUsageAndDoesNotStartBatch() {
    UnparserManager manager = mock(UnparserManager.class);
    UnparseCommand command = new UnparseCommand(manager);
    CommandArgs args = mock(CommandArgs.class);
    CommandSender sender = mock(CommandSender.class);

    when(args.length()).thenReturn(0);
    when(args.getSender()).thenReturn(sender);
    when(args.getLabel()).thenReturn("unmap");

    command.root(args);

    verify(sender).sendMessage(org.mockito.ArgumentMatchers.contains("Usage:"));
    verify(sender).sendMessage(org.mockito.ArgumentMatchers.contains("/unmap run"));
    verify(sender).sendMessage(org.mockito.ArgumentMatchers.contains("/unmap dryrun"));
    verify(sender).sendMessage(org.mockito.ArgumentMatchers.contains("/unmap status"));
    verify(sender).sendMessage(org.mockito.ArgumentMatchers.contains("/unmap help"));
    verify(manager, never())
        .processBatch(
            org.mockito.ArgumentMatchers.anyList(),
            anyString(),
            anyString(),
            org.mockito.ArgumentMatchers.anyBoolean(),
            org.mockito.ArgumentMatchers.any());
  }

  @Test
  void parseSourcesMergesQuotedTokens() throws Exception {
    UnparseCommand command = new UnparseCommand(mock(UnparserManager.class));
    Method parseSources = UnparseCommand.class.getDeclaredMethod("parseSources", String[].class);
    parseSources.setAccessible(true);

    String[] raw = new String[] {"\"worlds/Survival", "Games\"", "worlds/Dragon Escape"};
    @SuppressWarnings("unchecked")
    List<String> sources = (List<String>) parseSources.invoke(command, new Object[] {raw});

    assertEquals(2, sources.size());
    assertEquals("worlds/Survival Games", sources.get(0));
    assertEquals("worlds/Dragon Escape", sources.get(1));
  }

  @Test
  void completerSuggestsModesAtFirstArgument() {
    UnparseCommand command = new UnparseCommand(mock(UnparserManager.class));
    CommandArgs args = mock(CommandArgs.class);

    when(args.length()).thenReturn(1);
    when(args.getArgs(0)).thenReturn("d");

    List<String> suggestions = command.rootCompleter(args);

    assertTrue(suggestions.containsAll(Arrays.asList("dryrun")));
  }
}
