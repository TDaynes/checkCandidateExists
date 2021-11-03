package com.cheetah.api.verticle

import com.cheetah.api.service.CandidateService
import io.vertx.core.AbstractVerticle
import io.vertx.core.eventbus.MessageConsumer


public class CheckCandidateExistsVerticle extends AbstractVerticle {
  def candidateService = new CandidateService()

  @Override
  public void start(){
    println("CheckCandidateExistsVerticle - Deployed")

    MessageConsumer consumer = vertx.eventBus().consumer("com.cheetah.api.verticle.CheckCandidateExists")
    consumer.handler(message -> {

      def jedis = candidateService.connectToRedis()
      def activeCandidatesStringArray = candidateService.getActiveCandidates(jedis, "activeCandidates")
      def activeCandidates = []
      for(activeCandidateString in activeCandidatesStringArray){
        activeCandidates << Eval.me(activeCandidateString)
      }

      def searchParams = message.body().map
      def result = candidateService.checkCandidateExists(jedis, activeCandidates, searchParams)

      // TODO: Test with sleep
      sleep(1000)

      if(result["code"]==200){
        println("Check exists: " + searchParams?.id+", "+searchParams?.firstName+", "+searchParams?.lastName+" - "+result["candidateExists"])
      }
      else{
        println("Check exists: " + searchParams?.id+", "+searchParams?.firstName+", "+searchParams?.lastName+" - "+result["description"])
      }

      message.reply(result)
    })



  }

  public void stop(){

  }


}
