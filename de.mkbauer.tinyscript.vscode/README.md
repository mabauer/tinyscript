# tinyscript for Visual Studio Code

Syntax highlighting for [tinyscript](https://github.com/mabauer/tinyscript)
files (`.tys`): keywords, strings with escapes, numbers, comments, builtins
(`print`, `range`, `Math`, …), operators, class declarations; `finally`,
`switch`, `case`, `var`, `interface` and `implements` are marked as not
supported. Comment toggling, bracket matching and auto-closing quotes come from
the language configuration.

This is stage 1 (see `TODO.md` in the repository): no validation or completion
yet; those would come from a language server built from the Xtext grammar.

## Install

```bash
cd de.mkbauer.tinyscript.vscode
npm install
npm run package                     # creates tinyscript-0.3.0.vsix
code --install-extension tinyscript-0.3.0.vsix
# or into a profile:  code --install-extension tinyscript-0.3.0.vsix --profile Docs
```

To run a script, use the REPL from a terminal:
`de.mkbauer.tinyscript.repl/tinyscript file.tys`.

## Develop

- `syntaxes/tinyscript.tmLanguage.json` follows the keywords and terminals of
  `de.mkbauer.tinyscript/src/de/mkbauer/tinyscript/Tinyscript.xtext` and the
  builtins of `TinyscriptGlobalScopeProvider`; keep it in sync when the language
  changes.
- `npm test` checks the highlighting with
  [vscode-tmgrammar-test](https://github.com/PanAeon/vscode-tmgrammar-test):
  each line of `tests/syntax.tys` is followed by comment lines whose `^` marks
  columns and name the scope expected there.
- To try changes, open this folder in VS Code and press F5 (Extension
  Development Host).
