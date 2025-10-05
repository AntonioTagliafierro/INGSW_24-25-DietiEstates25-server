package com.data.models.offer

import com.data.models.user.User

interface  OfferDataSource {

    suspend fun createOffer(offer: Offer, firstMessage: OfferMessage): Boolean
    suspend fun addOfferMessage(offerId: String, newMessage: OfferMessage): Boolean
    suspend fun acceptOffer(offerId: String): Boolean
    suspend fun declineOffer(offerId: String): Boolean
    suspend fun getOffers(username: String, isAgent: Boolean): List<Offer>
    suspend fun getOffer(propertyId: String , buyerName: String): Offer?
    suspend fun getSummaryOffers(propertyId :String): List<OfferSummary>
    suspend fun getOffer(offerId : String): Offer?
}