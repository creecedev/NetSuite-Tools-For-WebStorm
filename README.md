# NetSuite Tools For WebStorm
NetSuite Tools For WebStorm, is an open-source WebStorm plugin that uses NetSuite SuiteTalk 2016.2 WSDL to provide the ability to:
  - Specify any NetSuite File Cabinet Folder as the project root directory or select from any folder within SuiteScripts or SuiteBundles
  - Upload single or groups of files to NetSuite File Cabinet
  - Compare local files against NetSuite File Cabinet

Available Options:
![Screenshot](https://plugins.jetbrains.com/files/8305/screenshot_15807.png)

# How To Use
### NetSuite Project Creation
When a project is initially created, the project must be setup with a login, password, environment and NetSuite File Cabinet root folder. These credentials are stored using IntelliJ IDEA Open API so they are securely handled. Once a project is successfully setup, all available actions will be present in the "NetSuite Tools" list which is available by right-clicking on the project.

*Please note that if you forget your master password, you will have to create a new one and go through the project setup again.*

### General Tips
- Keyboard shortcuts for available actions such as "Upload Selected File(s)" can be set in the IDE preferences under Keymap -> Plug-ins -> NetSuite Tools For WebStorm.
- Any external diff tool specified in the IDE preferences will be respected when comparing local files against the NetSuite File Cabinet. Otherwise, the default IDE diff tool will be used.

# How To Build
1. Create a new "Intellij Platform Plugin" Project and point it to the directory where the plugin code is located.
2. Open "Module Settings" on the project and on the "Dependencies" tab and add the project's "resources/dependencies" directory and check the "Export" checkbox so that the libraries are included with the plugin deployment.

# Resolving Issues
If you encounter any issues, please create an issue here on GitHub. I maintain this in my spare time and cannot always get to everything right away. Feel free to submit a pull request with any enhancements/bug fixes.

# Releases
NetSuite Tools For WebStorm is available in the [JetBrains Plugin Repository](https://plugins.jetbrains.com/plugin/8305?pr=idea) and I will periodically release when there is content to warrant a new release.

Licenses
----
This plugin's source code is under the MIT License.

Additional Licenses for dependencies:
   * Apache 2.0
      * Axis and jettison libraries as well as the IntelliJ Open API
   * NetSuite Application Developer License

----

About Me
----
[View My LinkedIn](https://www.linkedin.com/pub/chris-reece/118/853/424)
