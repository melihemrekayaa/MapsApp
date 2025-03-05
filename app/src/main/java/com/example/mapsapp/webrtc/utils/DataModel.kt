package com.example.mapsapp.webrtc.utils

enum class DataModelType {
    StartAudioCall, StartVideoCall, Offer, Answer, IceCandidates, EndCall, Unknown
}

data class DataModel(
    var sender: String? = null,
    var target: String? = null,
    var type: DataModelType = DataModelType.Unknown,
    var data: String? = null,
    var timeStamp: Long = System.currentTimeMillis()
) {
    // Firestore için boş constructor (Firebase nesneyi oluştururken bunu kullanır)
    constructor() : this(null, null, DataModelType.Unknown, null, 0L)
}

// DataModel nesnesinin geçerli olup olmadığını kontrol eden fonksiyon
fun DataModel.isValid(): Boolean {
    return System.currentTimeMillis() - this.timeStamp < 60000
}
