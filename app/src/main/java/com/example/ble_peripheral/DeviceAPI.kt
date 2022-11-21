package com.example.ble_peripheral

import kotlinx.coroutines.flow.Flow
import no.nordicsemi.android.ble.data.Data


interface DeviceAPI {
	/**
	 * Change the value of the GATT characteristic that we're publishing
	 */
	fun setMyCharacteristicValue(value: String)

	fun enableCallBacks(callback: (Data) ->Unit)
}