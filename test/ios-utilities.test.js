'use strict';

// getPlistPath builds cordova-ios's platform API inside the Cordova CLI's own process. It has to
// hand over the CLI's event emitter, or cordova-ios subscribes the console logger a second time
// and every later line of the build log is printed twice.

const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const test = require('node:test');

const common = require('cordova-common');
const Utilities = require('../plugin/scripts/ios/lib/utilities');

// Stands in for platforms/ios/cordova/Api.js and records how it was constructed.
const STUB_API = `
const path = require('path');

class Api {
  constructor(...args) {
    Api.calls.push(args);
    const platformRootDir = args[1];
    this.locations = { xcodeCordovaProj: path.join(platformRootDir, 'App') };
  }
}

Api.calls = [];
module.exports = Api;
`;

test('getPlistPath hands the CLI event emitter to the platform API', (t) => {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'fbsdk-hooks-'));
  t.after(() => fs.rmSync(root, { recursive: true, force: true }));

  const platformPath = path.join(root, 'platforms', 'ios');
  const apiPath = path.join(platformPath, 'cordova', 'Api.js');
  fs.writeFileSync(
    path.join(root, 'config.xml'),
    '<widget id="io.cordova.hooktest" version="1.0.0" xmlns="http://www.w3.org/ns/widgets"><name>HookTest</name></widget>\n',
  );
  fs.mkdirSync(path.dirname(apiPath), { recursive: true });
  fs.writeFileSync(apiPath, STUB_API);
  fs.mkdirSync(path.join(platformPath, 'App'));
  fs.writeFileSync(path.join(platformPath, 'App', 'App-Info.plist'), '');

  const context = {
    requireCordovaModule(name) {
      if (name === 'cordova-common') {
        return common;
      }
      if (name === 'cordova-lib/src/cordova/util') {
        return { isCordova: () => root, projectConfig: (projectRoot) => path.join(projectRoot, 'config.xml') };
      }
      throw new Error(`Unexpected module: ${name}`);
    },
  };

  assert.equal(Utilities.getPlistPath(context), path.join(platformPath, 'App', 'App-Info.plist'));

  const { calls } = require(apiPath);
  assert.equal(calls.length, 1);
  assert.equal(calls[0][2], common.events, 'the platform API was constructed without the CLI event emitter');
});
