/**
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
'use strict'

const functions = require('firebase-functions')
const admin = require('firebase-admin')
admin.initializeApp()

exports.sendMessage = functions.https
  .onCall(async (data, context) => {
    // [START_EXCLUDE]
    // [START readMessageData]
    // Message text passed from the client.
    const { fromId, toId, text } = data

    console.log('data: ', data)
    // [END readMessageData]
    // [START messageHttpsErrors]
    // Checking attribute.
    if (!(typeof text === 'string') || text.length === 0) {
      // Throwing an HttpsError so that the client gets the error details.
      throw new functions.https.HttpsError('invalid-argument', 'The function must be called with ' +
        'one arguments "text" containing the message text to add.')
    }
    // Checking that the user is authenticated.
    if (!context.auth) {
      // Throwing an HttpsError so that the client gets the error details.
      throw new functions.https.HttpsError('failed-precondition', 'The function must be called ' +
        'while authenticated.')
    }
    // [END messageHttpsErrors]

    // Get the list of device notification tokens.
    const getDeviceTokensPromise = admin.database()
      .ref(`/users/${toId}/notificationTokens`).once('value')

    // Get t, he follower profile.
    const getToUserProfilePromise = admin.auth().getUser(fromId)

    // The snapshot to the user's tokens.
    let tokensSnapshot

    // The array containing all the user's tokens.
    let tokens

    const results = await Promise.all([getDeviceTokensPromise, getToUserProfilePromise])
    tokensSnapshot = results[0]
    const toUser = results[1]

    // Check if there are any device tokens.
    if (!tokensSnapshot.hasChildren()) {
      return console.log('There are no notification tokens to send to.')
    }
    console.log('There are', tokensSnapshot.numChildren(), 'tokens to send notifications to.')
    console.log('Fetched message notification')

    // Notification details.
    const payload = {
      notification: {
        title: toUser.displayName,
        body: text,
        icon: "ic_notification",
        tag: "direct-message",
        sound: "default"
      },
      data: {
        photoURL: toUser.photoURL
      }
    }

    const options = {
      android: {
        priority: "normal"
      }
    }

    // Listing all tokens as an array.
    tokens = Object.keys(tokensSnapshot.val())

    // Send notifications to all tokens.
    const response = await admin.messaging().sendToDevice(tokens, payload, options)
    // For each message check if there was an error.
    const tokensToRemove = []
    response.results.forEach((result, index) => {
      const error = result.error
      if (error) {
        console.error('Failure sending notification to', tokens[index], error)
        // Cleanup the tokens who are not registered anymore.
        if (error.code === 'messaging/invalid-registration-token' ||
            error.code === 'messaging/registration-token-not-registered') {
          tokensToRemove.push(tokensSnapshot.ref.child(tokens[index]).remove())
        }
      }
    })
    return Promise.all(tokensToRemove)
  })
