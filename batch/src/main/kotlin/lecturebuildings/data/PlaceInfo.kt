package com.wafflestudio.snu4t.lecturebuildings.data

class PlaceInfo(
    val rawString: String,
    val campus: Campus,
    val buildingNumber: String,
) {
    companion object {
        fun getValuesOf(places: String): List<PlaceInfo> = places.split("/").mapNotNull { PlaceInfo(it) }
    }
}

fun PlaceInfo(place: String): PlaceInfo? {
    val campus: Campus = when (place.first()) {
        '#' -> Campus.YEONGEON
        '*' -> Campus.PYEONGCHANG
        else -> Campus.GWANAK
    }

    val placeWithOutCampus = place.removePrefix("#").removePrefix("*")
    val splits = placeWithOutCampus.split("-").filter { !it.matches("^[A-Za-z]*$".toRegex()) }

    val buildingNumber = when (splits.count()) {
        3 -> if (splits[1].count() == 1) splits.dropLast(1).joinToString("-") else splits.first()
        else -> splits.firstOrNull()
    }?.let {
        it.trimStart { firstChar -> firstChar == '0' }
    } ?: return null

    return PlaceInfo(place, campus, buildingNumber)
}
