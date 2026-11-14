//
//  FacebookConnectPlugin.h
//  GapFacebookConnect
//
//  Created by Jesse MacFadyen on 11-04-22.
//  Updated by Ally Ogilvie on 29/Jan/2014.
//  Updated by Jeduan Cornejo on 3/Jul/2015
//  Updated by David Dal Busco on 21/Apr/2018 - Facebook doesn't support App Invites anymore
//  Copyright 2011 Nitobi. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <FBSDKCoreKit/FBSDKCoreKit.h>
#import <FBSDKLoginKit/FBSDKLoginKit.h>
#import <FBSDKShareKit/FBSDKShareKit.h>
#import <FBSDKGamingServicesKit/FBSDKGamingServicesKit-Swift.h>
#import <Cordova/CDV.h>
#import "AppDelegate.h"

@interface FacebookConnectPlugin : CDVPlugin <FBSDKSharingDelegate, FBSDKGameRequestDialogDelegate>
- (void)getApplicationId:(CDVInvokedUrlCommand *)command;
- (void)setApplicationId:(CDVInvokedUrlCommand *)command;
- (void)getClientToken:(CDVInvokedUrlCommand *)command;
- (void)setClientToken:(CDVInvokedUrlCommand *)command;
- (void)getApplicationName:(CDVInvokedUrlCommand *)command;
- (void)setApplicationName:(CDVInvokedUrlCommand *)command;
- (void)getLoginStatus:(CDVInvokedUrlCommand *)command;
- (void)getAccessToken:(CDVInvokedUrlCommand *)command;
- (void)setAutoInitEnabled:(CDVInvokedUrlCommand *)command;
- (void)setAutoLogAppEventsEnabled:(CDVInvokedUrlCommand *)command;
- (void)setAdvertiserIDCollectionEnabled:(CDVInvokedUrlCommand *)command;
- (void)setAdvertiserTrackingEnabled:(CDVInvokedUrlCommand *)command;
- (void)setDataProcessingOptions:(CDVInvokedUrlCommand *)command;
- (void)setUserData:(CDVInvokedUrlCommand *)command;
- (void)clearUserData:(CDVInvokedUrlCommand *)command;
- (void)logEvent:(CDVInvokedUrlCommand *)command;
- (void)logPurchase:(CDVInvokedUrlCommand *)command;
- (void)login:(CDVInvokedUrlCommand *)command;
- (void)loginWithLimitedTracking:(CDVInvokedUrlCommand *)command;
- (void)checkHasCorrectPermissions:(CDVInvokedUrlCommand *)command;
- (void)isDataAccessExpired:(CDVInvokedUrlCommand *)command;
- (void)reauthorizeDataAccess:(CDVInvokedUrlCommand *)command;
- (void)logout:(CDVInvokedUrlCommand *)command;
- (void)getCurrentProfile:(CDVInvokedUrlCommand *)command;
- (void)graphApi:(CDVInvokedUrlCommand *)command;
- (void)showDialog:(CDVInvokedUrlCommand *)command;
- (void)getDeferredApplink:(CDVInvokedUrlCommand *) command;
- (void)activateApp:(CDVInvokedUrlCommand *)command;
@end
