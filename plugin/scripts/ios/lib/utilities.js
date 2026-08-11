const fs = require('fs');
const path = require('path');
const Utilities = {};

Utilities.getPreferenceValueFromConfig = function (config, name) {
  const value = config.match(new RegExp('name="' + name + '" value="(.*?)"', "i"))
  if (value && value[1]) {
    return value[1]
  } else {
    return null
  }
}

Utilities.getPreferenceValueFromPackageJson = function (packageJson, name) {
  const value = packageJson.match(new RegExp('"' + name + '":\\s"(.*?)"', "i"));
  if (value && value[1]) {
    return value[1]
  } else {
    return null
  }
}

Utilities.getPreferenceValue = function (name) {
  const config = fs.readFileSync("config.xml").toString();
  let preferenceValue = Utilities.getPreferenceValueFromConfig(config, name);
  if (!preferenceValue) {
    const packageJson = fs.readFileSync("package.json").toString();
    preferenceValue = Utilities.getPreferenceValueFromPackageJson(packageJson, name)
  }
  return preferenceValue
}

Utilities.getPlistPath = function (context) {
  const common = context.requireCordovaModule('cordova-common');
  const util = context.requireCordovaModule('cordova-lib/src/cordova/util');
  const projectRoot = util.isCordova();
  const platformPath = path.join(projectRoot, 'platforms', 'ios');

  // Since cordova-ios 8.0.0 the Xcode project and target are always named "App", regardless of
  // <name> in config.xml, so the <name>-derived path below no longer exists. Ask the installed
  // platform where its project lives rather than guessing: platforms/ios/cordova/Api.js is the
  // same API the Cordova CLI drives it through, and locations.xcodeCordovaProj is what
  // cordova-ios itself uses when it writes that plist. That keeps this correct if the directory
  // is ever renamed again.
  try {
    const PlatformApi = require(path.join(platformPath, 'cordova', 'Api.js'));
    const plistPath = path.join(new PlatformApi('ios', platformPath).locations.xcodeCordovaProj, 'App-Info.plist');
    if (fs.existsSync(plistPath)) {
      return plistPath
    }
  } catch (e) {
    // Platform not installed yet, or older than the Api.js layout - fall through.
  }

  const projectName = new common.ConfigParser(util.projectConfig(projectRoot)).name();
  return path.join(platformPath, projectName, projectName + '-Info.plist')
}

module.exports = Utilities;
