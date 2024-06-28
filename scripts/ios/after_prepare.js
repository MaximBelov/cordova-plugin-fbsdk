'use strict';

const fs = require('fs');
const utilities = require('./lib/utilities');

module.exports = function (context) {

  // 1. Prepare values

  let FACEBOOK_URL_SCHEME_SUFFIX = ' '
  if (process.argv.join("|").indexOf("FACEBOOK_URL_SCHEME_SUFFIX=") > -1) {
    FACEBOOK_URL_SCHEME_SUFFIX = process.argv.join("|").match(/FACEBOOK_URL_SCHEME_SUFFIX=(.*?)(\||$)/)[1]
  } else {
    FACEBOOK_URL_SCHEME_SUFFIX = utilities.getPreferenceValue("FACEBOOK_URL_SCHEME_SUFFIX")
  }
  if (!FACEBOOK_URL_SCHEME_SUFFIX || FACEBOOK_URL_SCHEME_SUFFIX === ' ') {
    FACEBOOK_URL_SCHEME_SUFFIX = ''
  }

  let OTHER_APP_SCHEMES = ' '
  if (process.argv.join("|").indexOf("OTHER_APP_SCHEMES=") > -1) {
    OTHER_APP_SCHEMES = process.argv.join("|").match(/OTHER_APP_SCHEMES=(.*?)(\||$)/)[1]
  } else {
    OTHER_APP_SCHEMES = utilities.getPreferenceValue("OTHER_APP_SCHEMES")
  }
  if (!OTHER_APP_SCHEMES || OTHER_APP_SCHEMES === ' ') {
    OTHER_APP_SCHEMES = ''
  }

  let FACEBOOK_AUTO_LOG_APP_EVENTS = 'true'
  if (process.argv.join("|").indexOf("FACEBOOK_AUTO_LOG_APP_EVENTS=") > -1) {
    FACEBOOK_AUTO_LOG_APP_EVENTS = process.argv.join("|").match(/FACEBOOK_AUTO_LOG_APP_EVENTS=(.*?)(\||$)/)[1]
  } else {
    FACEBOOK_AUTO_LOG_APP_EVENTS = utilities.getPreferenceValue("FACEBOOK_AUTO_LOG_APP_EVENTS")
  }
  if (typeof FACEBOOK_AUTO_LOG_APP_EVENTS == 'string' && FACEBOOK_AUTO_LOG_APP_EVENTS.toLowerCase() === 'false') {
    FACEBOOK_AUTO_LOG_APP_EVENTS = 'false'
  } else {
    FACEBOOK_AUTO_LOG_APP_EVENTS = 'true'
  }

  let FACEBOOK_ADVERTISER_ID_COLLECTION = 'true'
  if (process.argv.join("|").indexOf("FACEBOOK_ADVERTISER_ID_COLLECTION=") > -1) {
    FACEBOOK_ADVERTISER_ID_COLLECTION = process.argv.join("|").match(/FACEBOOK_ADVERTISER_ID_COLLECTION=(.*?)(\||$)/)[1]
  } else {
    FACEBOOK_ADVERTISER_ID_COLLECTION = utilities.getPreferenceValue("FACEBOOK_ADVERTISER_ID_COLLECTION")
  }
  if (typeof FACEBOOK_ADVERTISER_ID_COLLECTION == 'string' && FACEBOOK_ADVERTISER_ID_COLLECTION.toLowerCase() === 'false') {
    FACEBOOK_ADVERTISER_ID_COLLECTION = 'false'
  } else {
    FACEBOOK_ADVERTISER_ID_COLLECTION = 'true'
  }

  // 2. Update  values

  const updatePlistContent = function () {
    const plistPath = utilities.getPlistPath(context)
    fs.statSync(plistPath)

    var plistContent = fs.readFileSync(plistPath, 'utf8')

    if (FACEBOOK_URL_SCHEME_SUFFIX === '') {
      plistContent = plistContent.replace('<key>FacebookUrlSchemeSuffix</key>', '').replace('<string>FACEBOOK_URL_SCHEME_SUFFIX_PLACEHOLDER</string>', '')
    }
    plistContent = plistContent.replace(/FACEBOOK_URL_SCHEME_SUFFIX_PLACEHOLDER/g, FACEBOOK_URL_SCHEME_SUFFIX)

    if (OTHER_APP_SCHEMES === '') {
      plistContent = plistContent.replace('<string>OTHER_APP_SCHEMES_PLACEHOLDER</string>', '')
    } else {
      var otherAppSchemeStrings = OTHER_APP_SCHEMES.replace(/,/g, '</string><string>')
      plistContent = plistContent.replace('OTHER_APP_SCHEMES_PLACEHOLDER', otherAppSchemeStrings)
    }

    if (plistContent.indexOf('<key>FacebookAutoLogAppEventsEnabled</key>') === -1) {
      plistContent = plistContent.replace('<key>FacebookAutoLogAppEventsEnabled_PLACEHOLDER</key>', '<key>FacebookAutoLogAppEventsEnabled</key>').replace('<string>FACEBOOK_AUTO_LOG_APP_EVENTS_PLACEHOLDER</string>', '<' + FACEBOOK_AUTO_LOG_APP_EVENTS + ' />')
    } else {
      plistContent = plistContent.replace('<key>FacebookAutoLogAppEventsEnabled_PLACEHOLDER</key>', '').replace('<string>FACEBOOK_AUTO_LOG_APP_EVENTS_PLACEHOLDER</string>', '')
    }

    if (plistContent.indexOf('<key>FacebookAdvertiserIDCollectionEnabled</key>') === -1) {
      plistContent = plistContent.replace('<key>FacebookAdvertiserIDCollectionEnabled_PLACEHOLDER</key>', '<key>FacebookAdvertiserIDCollectionEnabled</key>').replace('<string>FACEBOOK_ADVERTISER_ID_COLLECTION_PLACEHOLDER</string>', '<' + FACEBOOK_ADVERTISER_ID_COLLECTION + ' />')
    } else {
      plistContent = plistContent.replace('<key>FacebookAdvertiserIDCollectionEnabled_PLACEHOLDER</key>', '').replace('<string>FACEBOOK_ADVERTISER_ID_COLLECTION_PLACEHOLDER</string>', '')
    }

    fs.writeFileSync(plistPath, plistContent, 'utf8')
  }

  updatePlistContent()
}
