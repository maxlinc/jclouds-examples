/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jclouds.examples.rackspace.clouddns;

import com.google.common.collect.Iterables;
import com.google.common.io.Closeables;
import org.jclouds.ContextBuilder;
import org.jclouds.rackspace.clouddns.v1.CloudDNSApi;
import org.jclouds.rackspace.clouddns.v1.domain.Domain;
import org.jclouds.rackspace.clouddns.v1.domain.RecordDetail;

import java.io.Closeable;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import static org.jclouds.examples.rackspace.clouddns.Constants.ALT_NAME;
import static org.jclouds.examples.rackspace.clouddns.Constants.PROVIDER;
import static org.jclouds.rackspace.clouddns.v1.functions.RecordFunctions.GET_RECORD_ID;
import static org.jclouds.rackspace.clouddns.v1.predicates.JobPredicates.awaitComplete;

/**
 * This example deletes the records on a domain. 
 *  
 */
public class DeleteRecords implements Closeable {
   private final CloudDNSApi dnsApi;

   /**
    * To get a username and API key see http://www.jclouds.org/documentation/quickstart/rackspace/
    * 
    * The first argument (args[0]) must be your username
    * The second argument (args[1]) must be your API key
    */
   public static void main(String[] args) throws IOException {
      DeleteRecords deleteRecords = new DeleteRecords(args[0], args[1]);

      try {
         Domain domain = deleteRecords.getDomain();
         deleteRecords.deleteRecords(domain);
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      finally {
         deleteRecords.close();
      }
   }

   public DeleteRecords(String username, String apiKey) {
      dnsApi = ContextBuilder.newBuilder(PROVIDER)
            .credentials(username, apiKey)
            .buildApi(CloudDNSApi.class);
   }

   private Domain getDomain() {
      for (Domain domain: dnsApi.getDomainApi().list().concat()) {
         if (domain.getName().startsWith(ALT_NAME)) {
            return domain;
         }
      }
      
      throw new RuntimeException(ALT_NAME + " not found. Run CreateDomains example first.");
   }

   private void deleteRecords(Domain domain) throws TimeoutException {
      System.out.format("Delete Records%n");
      
      Set<RecordDetail> recordDetails = dnsApi.getRecordApiForDomain(domain.getId()).listByType("TXT").concat().toSet();
      Iterable<String> recordIds = Iterables.transform(recordDetails, GET_RECORD_ID);
      
      awaitComplete(dnsApi, dnsApi.getRecordApiForDomain(domain.getId()).delete(recordIds));
      
      System.out.format("  Deleted %s records", Iterables.size(recordDetails));
   }
   
   /**
    * Always close your service when you're done with it.
    * 
    * Note that closing quietly like this is not necessary in Java 7. 
    * You would use try-with-resources in the main method instead.
    */
   public void close() throws IOException {
      Closeables.close(dnsApi, true);
   }
}
