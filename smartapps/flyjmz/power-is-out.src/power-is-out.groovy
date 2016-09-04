/**
 *  Power Is Out
 *
 *  Copyright 2014 flyjmz
 *
 *	https://github.com/flyjmz/jmzSmartThings
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *	Version 1.1		2 August 2016 		- updated notes within app to clarify.
 *	Version 1.2	 	4 August 2016 		- created ability to turn on/off lights/switches (like Cree bulbs, Hue, etc.) when power is restored.  Based on inputs from scottinpollock.
 * 	Version 1.3		3 September 2016	- Cleaned up code a little
 */
 
definition(
    name: "Power Is Out",
    namespace: "flyjmz",
    author: "flyjmz",
    description: "Alerts when power lost (uses SmartSense Motion v1's change from wired-power to battery-power)",
    category: "Safety & Security",
    iconUrl: "https://github.com/flyjmz/jmzSmartThings/raw/master/resources/home2-icn@2x.png",
    iconX2Url: "https://github.com/flyjmz/jmzSmartThings/raw/master/resources/home2-icn@2x.png",
    iconX3Url: "https://github.com/flyjmz/jmzSmartThings/raw/master/resources/home2-icn@2x.png"
)


preferences {
	section("When there is wired-power loss on...") {
			input "motion1", "capability.motionSensor", title: "Where?"
            paragraph "Must be a SmartSense Motion v1.  Also, SmartThings Hub and internet connection (modem/router) must retain power for this to work (e.g. connect them to a UPS)."
	}
	section("Enter Phone number if you want a text message (optional) as well as a push notification."){         //TO DO - Add periodic notifications!
    	input "pushAndPhone", "enum", title: "Send SMS?", required: false, metadata: [values: ["Yes","No"]]
		input "phone1", "phone", title: "Phone Number (only for SMS)", required: false
	}
    section("Make changes to the following when powered is restored..."){
        input "offSwitches", "capability.switch", title: "Turn these off", required: false, multiple: true
    	input "onSwitchesAlways", "capability.switch", title: "Turn these on", required: false, multiple: true
       	input "onSwitchesDark", "capability.switch", title: "Turn these on if after sunset", required: false, multiple: true
       	input "waitSeconds", "number", title: "How long to wait before conducting changes (in seconds)?  (To ensure devices have reconnected to hub)", required: true, defaultValue: 30
    }
}

def installed() {
	unsubscribe()
    subscribe(motion1, "powerSource.battery", onBatteryPowerHandler)
    subscribe(motion1, "powerSource.powered", PoweredPowerHandler)
}

def updated() {
	unsubscribe()
	subscribe(motion1, "powerSource.battery", onBatteryPowerHandler)
    subscribe(motion1, "powerSource.powered", PoweredPowerHandler)
}

def onBatteryPowerHandler(evt) {
	log.trace "$evt.value: $evt"
	def msg = "${motion1.label ?: motion1.name} sensed Power is Out!"
    
	log.debug "sending push for power is out"
	sendPush(msg)
    
    if ( phone1 && pushAndPhone ) {
    	log.debug "sending SMS to ${phone1}"
   	sendSms(phone1, msg)
	}
}

def PoweredPowerHandler(evt) {
	log.trace "$evt.value: $evt"
	def msg = "${motion1.label ?: motion1.name} sensed Power is Back On!"
	runIn(waitSeconds,switchChanger)  //wait a period of time to ensure the things you're trying to change are connected to hub again
    log.debug "sending push for power is back on"
	sendPush(msg)
    if ( phone1 && pushAndPhone ) {
    	log.debug "sending SMS to ${phone1}"
    	sendSms(phone1, msg)
	}
}

def switchChanger() {
	if (offSwitches) {
    	log.debug "turning off switches now that power is restored"
    	offSwitches.off()
	}
    if (onSwitchesDark) {
    	log.debug "turning on switches now that power is restored and it's dark"
        def ss = getSunriseAndSunset()
        def now = new Date()
		def dark = ss.sunset
        if (dark.before(now)) {
    		onSwitchesDark.on()
        }    
	}
    if (onSwitchesAlways) {
    	log.debug "turning on switches now that power is restored"
    	onSwitchesAlways.on()
	}
}