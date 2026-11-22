package de.mkbauer.tinyscript.repl;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import de.mkbauer.tinyscript.interpreter.TSNumber;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSString;
import de.mkbauer.tinyscript.interpreter.TSValue;
import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;

/**
 * Tab completion in the interactive session: the names of the session, the builtins and the
 * keywords; after a dot the properties of the object (with those of its prototypes), e.g.
 * {@code Math.} offers {@code max}, {@code min}, ... Only names are looked up, nothing is
 * called, so completing has no effects.
 */
final class TinyscriptCompleter implements Completer {

	/** What numbers have (see TinyscriptEngine: numbers only have these methods). */
	private static final List<String> NUMBER_METHODS = List.of("toFixed", "toString");

	private final Supplier<TinyscriptEngine> engine;

	TinyscriptCompleter(Supplier<TinyscriptEngine> engine) {
		this.engine = engine;
	}

	@Override
	public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
		String word = line.word();
		int dot = word.lastIndexOf('.');
		for (String completion : completions(word))
			candidates.add(new Candidate(completion, completion.substring(dot + 1), null, null, null, null, false));
	}

	/**
	 * The completions of a word (as the word they replace it with), unsorted.
	 */
	List<String> completions(String word) {
		if (word.startsWith("!"))
			return TinyscriptSyntax.COMMANDS.stream().filter(command -> command.startsWith(word)).toList();
		int dot = word.lastIndexOf('.');
		if (dot < 0) {
			Set<String> names = new LinkedHashSet<>(namesOf(engine.get().getGlobalContext().getGlobalObject()));
			names.addAll(TinyscriptSyntax.KEYWORDS);
			names.addAll(TinyscriptSyntax.ATOMS);
			return names.stream().filter(name -> name.startsWith(word)).toList();
		}
		String prefix = word.substring(0, dot + 1);
		String start = word.substring(dot + 1);
		TSValue value = valueOf(word.substring(0, dot));
		List<String> result = new ArrayList<>();
		for (String name : propertiesOf(value)) {
			if (name.startsWith(start))
				result.add(prefix + name);
		}
		return result;
	}

	/**
	 * The value of a path of names (a.b.c), or undefined if a part of it does not exist.
	 */
	private TSValue valueOf(String path) {
		TSValue value = null;
		for (String part : path.split("\\.")) {
			String name = part.strip();
			if (value == null)
				value = engine.get().getGlobalContext().getGlobalObject().get(name);
			else if (value instanceof TSObject object)
				value = object.get(name);
			else
				return TSValue.UNDEFINED;
		}
		return value == null ? TSValue.UNDEFINED : value;
	}

	private List<String> propertiesOf(TSValue value) {
		return switch (value) {
			case TSObject object -> namesOf(object);
			case TSString string -> namesOf(TSObject.toObject(engine.get(), string));
			case TSNumber number -> NUMBER_METHODS;
			default -> List.of();
		};
	}

	/**
	 * The names of the properties of an object and its prototypes that are names (not 0 or "a b").
	 */
	private static List<String> namesOf(TSObject object) {
		Set<String> names = new LinkedHashSet<>();
		for (TSObject current = object; current != null; current = current.getPrototype()) {
			for (String name : current.getOwnPropertyNames()) {
				if (name.matches("[A-Za-z_$][\\w$]*") && !name.equals("constructor"))
					names.add(name);
			}
		}
		return List.copyOf(names);
	}

}
