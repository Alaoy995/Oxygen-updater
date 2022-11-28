# Contributing

## Code

Thanks for contributing to Oxygen Updater, the largest community-driven app for OnePlus devices.
To help things stay organized, there are some rules that you'll have to follow:

- Make sure your code works on an actual OnePlus device, not just the Android Emulator
- Make sure your code works when you run the app with the "release" profile. This ensures that the ProGuard minification does not break the code.
(You may need to generate a new keystore and change the password in `build.gradle` for this to work)
- Clearly indicate your changes in the commit message
- Open a PR at GitHub.

## Translations

### Requirements

- An IDE or text editor
- A GitHub account

### Updating an existing language translation

1. If you want to update an existing language, go to <https://github.com/oxygen-updater/oxygen-updater/tree/master/app/src/main/res>. There, you’ll notice there are a lot of folders named “values-[country initials]”. For example, “fr” = France, so that’ll be named “values-fr”. With The Netherlands this would be “values-[nl]”. Don’t know what your country’s initials are? Check this page: <https://en.wikipedia.org/wiki/List_of_ISO_639-1_codes>.
2. Fork the repository (can be done via <https://github.com/oxygen-updater/oxygen-updater>) or via your IDE. Use the following naming convention, for example: `translation/Dutch`. So if the language you want to translate is Danish, use `translation/Danish` as branch name.
3. Make the changes in the correct folder: location is [app\src\main\res](app\src\main\res) and commit to the branch you just created. After you've finished translating, create a Pull Request.

### Translating a new language

1. If you want to translate a new language, go to <https://github.com/oxygen-updater/oxygen-updater/tree/master/app/src/main/res>. There, you’ll notice there are a lot of folders named “values-[country initials]”. For example, “fr” = France, so that’ll be named “values-fr”. With The Netherlands this would be “values-[nl]”. Don’t know what your country’s initials are? Check this page: <https://en.wikipedia.org/wiki/List_of_ISO_639-1_codes>.
2. Fork the repository (can be done via <https://github.com/oxygen-updater/oxygen-updater>) or via your IDE. Use the following naming convention, for example: `translation/Dutch`. So if the language you want to translate is Danish, use `translation/Danish` as branch name.
3. Head to [app\src\main\res](app\src\main\res) and create a new folder with the same name convention like the other translation files, like `values-nl` for Dutch.
4. Inside the folder, create a file named `strings.xml` and paste the following content inside that file: <https://raw.githubusercontent.com/oxygen-updater/oxygen-updater/master/app/src/main/res/values/strings.xml>. Now, you can start translating. Commit to the branch you just created and after that, open a Pull Request.
