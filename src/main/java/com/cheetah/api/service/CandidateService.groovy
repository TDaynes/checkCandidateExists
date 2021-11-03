package com.cheetah.api.service

import io.vertx.core.json.JsonObject
import orestes.bloomfilter.BloomFilter
import orestes.bloomfilter.FilterBuilder
import org.json.simple.JSONObject
import org.msgpack.util.json.JSON
import redis.clients.jedis.Jedis

class CandidateService {


  def connectToRedis(){
    def jedis = new Jedis("localhost")

    return jedis
  }


  // Would normally have setters and getters here


  def getActiveCandidates(jedis, tableName){
    def candidateCount = jedis.llen(tableName)
    def candidates = jedis.lrange(tableName, 0, candidateCount)
    return candidates
  }


  def checkCandidateExists(jedis, activeCandidates, searchParams){
    if(!activeCandidates){
      return new JsonObject().put("code", 400).put("description", "Request missing activeCandidates.")
    }

    String candidateId = searchParams?.id?: ""
    String firstName = searchParams?.firstName?: ""
    String lastName = searchParams?.lastName?: ""
    if(candidateId=="" && firstName=="" && lastName==""){
      return new JsonObject().put("code", 400).put("description", "Request missing fields.")
    }

    def searchType = "default"
    if(candidateId!="" && firstName=="" && lastName==""){ searchType = "idOnly" }
    else if(candidateId=="" && firstName!="" && lastName!=""){ searchType = "nameOnly" }

    def candidateExists = false
    def bloomFilter = new FilterBuilder(1000, 0.1).buildBloomFilter()

    if(searchType=="default"){
      //default search: id, firstName and lastName
      bloomFilter.addAll(activeCandidates)
      candidateExists = bloomFilter.contains(searchParams)
    }
    else if(searchType=="idOnly"){
      bloomFilter.addAll(activeCandidates?.id)
      candidateExists = bloomFilter.contains(candidateId)
    }
    else if(searchType=="nameOnly"){
      for(activeCandidate in activeCandidates){
        bloomFilter.add(activeCandidate.firstName+" "+activeCandidate.lastName)
      }
      candidateExists = bloomFilter.contains(firstName+" "+lastName)
    }

    // TODO: Could put 2nd check, as a database query, if candidateExists==true to be 100% sure of accuracy

    return new JsonObject().put("code", 200).put("candidateExists", candidateExists)
  }



}
