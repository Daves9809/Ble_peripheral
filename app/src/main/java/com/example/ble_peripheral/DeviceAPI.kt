package com.example.ble_peripheral

import kotlinx.coroutines.flow.Flow


interface DeviceAPI {
	/**
	 * Change the value of the GATT characteristic that we're publishing
	 */
	fun setMyCharacteristicValue(value: String)


}