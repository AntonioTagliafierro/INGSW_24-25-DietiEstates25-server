package com.data.models.offer

interface  OfferDataSource {

    suspend fun createOffer(offer: Offer, firstMessage: OfferMessage): Boolean
    suspend fun addOfferMessage(offerId: String, newMessage: OfferMessage): Boolean
    suspend fun acceptOffer(offerId: String): Boolean
    suspend fun declineOffer(offerId: String): Boolean
    suspend fun getAllOffers(): List<OfferSummary>
    suspend fun getOffersByUserOrAgent(userId: String): List<Offer>
    suspend fun getOfferByPropertyAndBuyer(propertyId: String, buyerId: String): Offer?
}