# Translating CAS Calculator

The app is written in English, and every string it shows goes through `tr()` (`ui/I18n.kt`).
A translation is one plain text file, read when the app starts:

1. Copy `template.tsv` to `app/src/main/assets/i18n/<code>.tsv`, where `<code>` is the
   language code Android uses (`fy` Frisian, `sr` Serbian, `hr` Croatian, `de` German…).
2. Put the language's name, written in itself, on the first line after `#name` and a tab.
3. Write each translation after the tab on its line. A line left empty stays English.

As soon as one file is there, Settings › Language appears, offering English, your language,
or the phone's language. Keep `{0}`, `{1}`… (the app fills them in) and `$…$` math as they
are; `\n` is a new line. `TranslationsTest` checks every line matches a string the app shows
and keeps its placeholders, so a renamed string is caught.

The template is made from the app's strings; the documentation and the key explanations
aren't in it yet and stay English.
