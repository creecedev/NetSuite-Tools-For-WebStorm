# NetSuite Tools For WebStorm
NetSuite Tools For WebStorm, is an open-source WebStorm plugin that uses NetSuite SuiteTalk 2016.1 WSDL to provide the ability to:
  - Specify any NetSuite File Cabinet Folder as the project root directory or select form SuiteScripts or SuiteBundles listing
  - Upload Files to NetSuite File Cabinet
  - Compare local File(s) against NetSuite File Cabinet

![Screenshot](https://plugins.jetbrains.com/files/8305/screenshot_15807.png)

# How To Use
This plugin functions extremely similar to the Eclipse plugin in that all available actions are available in a right-click menu under "NetSuite Tools". When you initially, create a project, you must first setup the project with login/pass/environment, select an account and then specify a root folder for which all actions will be taken relative to. Once the root folder is specified, the project settings are saved. Passwords are stored using the built in Master Password functionality encryption. Once a project is setup, all available action will be present in the right-click menu under "NetSuite Tools".

# How To Build
- Create new Intellij Plugin Project and point to checked out code directory
- Add all axis .jar files under the resources/axis_1-4 as dependencies in "Module Settings"
- Add jettison-1.3.2.jar under /Applications/IntelliJ IDEA.app/Contents/lib or wherever you download it and place it as a dependency in "Module Settings"
- Add nsws-2016_1.jar under resources/ as a dependency in "Module Settings"

# Resolving Issues
If you encounter any issues, please create an issue. I maintain this in my free time so helping correct an issue is much appreciated.

# Known Issues
- There needs to be some code cleanup and some better error handling which I am aware of. 
- Occasionally, an error is thrown about passing things between context (ususally a Project reference). I have resolved most of these but there could be 1 or 2 I haven't seen yet. They usually pop up once and will not interefere with functionality.

# Releases
NetSuite Tools For WebStorm is available in the [JetBrains Plugin Repository](https://plugins.jetbrains.com/plugin/8305?pr=idea) and I will periodically release when there is content to warrant a new release.

Licenses
----
MIT

NetSuite Application Developer License
----
NetSuite Application Developer License is included in /src and must be agreed to

About Me
----
[View My LinkedIn](https://www.linkedin.com/pub/chris-reece/118/853/424)