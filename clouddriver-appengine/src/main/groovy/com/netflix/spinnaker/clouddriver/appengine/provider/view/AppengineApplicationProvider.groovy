/*
 * Copyright 2016 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.clouddriver.appengine.provider.view

import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spinnaker.cats.cache.Cache
import com.netflix.spinnaker.cats.cache.CacheData
import com.netflix.spinnaker.cats.cache.RelationshipCacheFilter
import com.netflix.spinnaker.clouddriver.appengine.AppengineCloudProvider
import com.netflix.spinnaker.clouddriver.appengine.cache.Keys
import com.netflix.spinnaker.clouddriver.appengine.model.AppengineApplication
import com.netflix.spinnaker.clouddriver.model.ApplicationProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import static com.netflix.spinnaker.clouddriver.appengine.cache.Keys.Namespace.APPLICATIONS
import static com.netflix.spinnaker.clouddriver.appengine.cache.Keys.Namespace.CLUSTERS

@Component
class AppengineApplicationProvider implements ApplicationProvider {
  @Autowired
  Cache cacheView

  @Autowired
  ObjectMapper objectMapper

  @Override
  Set<AppengineApplication> getApplications(boolean expand) {
    def filter = expand ? RelationshipCacheFilter.include(CLUSTERS.ns) : RelationshipCacheFilter.none()
    cacheView.getAll(APPLICATIONS.ns,
                     cacheView.filterIdentifiers(APPLICATIONS.ns, "${AppengineCloudProvider.ID}:*"),
                     filter).collect { applicationFromCacheData(it) } as Set
  }

  @Override
  AppengineApplication getApplication(String name) {
    CacheData cacheData = cacheView.get(APPLICATIONS.ns,
                                        Keys.getApplicationKey(name),
                                        RelationshipCacheFilter.include(CLUSTERS.ns))

    cacheData ? applicationFromCacheData(cacheData) : null
  }

  AppengineApplication applicationFromCacheData (CacheData cacheData) {
    AppengineApplication application = objectMapper.convertValue(cacheData.attributes, AppengineApplication)

    cacheData.relationships[CLUSTERS.ns].each { String clusterKey ->
      def parsedClusterKey = Keys.parse(clusterKey)
      application.clusterNames[parsedClusterKey.account] << parsedClusterKey.name
    }
    application
  }
}
