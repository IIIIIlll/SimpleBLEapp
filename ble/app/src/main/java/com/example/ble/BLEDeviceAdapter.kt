package com.example.ble

import android.annotation.SuppressLint
import android.bluetooth.le.ScanResult
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

class BLEDeviceAdapter(private val devices: List<ScanResult>) : BaseAdapter() {

    @SuppressLint("MissingPermission")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View
        val viewHolder: ViewHolder

        if (convertView == null) {
            view = LayoutInflater.from(parent?.context).inflate(android.R.layout.simple_list_item_2, parent, false)
            viewHolder = ViewHolder(view)
            view.tag = viewHolder
        } else {
            view = convertView
            viewHolder = view.tag as ViewHolder
        }

        val device = devices[position]
        viewHolder.deviceName.text = device.device.name ?: "Unknown Device"
        viewHolder.deviceAddress.text = device.device.address

        return view
    }

    override fun getItem(position: Int): Any {
        return devices[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getCount(): Int {
        return devices.size
    }

    private class ViewHolder(view: View) {
        val deviceName: TextView = view.findViewById(android.R.id.text1)
        val deviceAddress: TextView = view.findViewById(android.R.id.text2)
    }
}
