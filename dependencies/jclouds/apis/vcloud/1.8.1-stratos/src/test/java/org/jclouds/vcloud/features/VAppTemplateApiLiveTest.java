/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jclouds.vcloud.features;

import static com.google.common.collect.Iterables.getOnlyElement;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.jclouds.util.Predicates2.retry;
import static org.testng.Assert.assertEquals;

import java.net.URI;

import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.vcloud.VCloudMediaType;
import org.jclouds.vcloud.domain.Catalog;
import org.jclouds.vcloud.domain.CatalogItem;
import org.jclouds.vcloud.domain.Org;
import org.jclouds.vcloud.domain.ReferenceType;
import org.jclouds.vcloud.domain.Status;
import org.jclouds.vcloud.domain.Task;
import org.jclouds.vcloud.domain.VApp;
import org.jclouds.vcloud.domain.VAppTemplate;
import org.jclouds.vcloud.internal.BaseVCloudApiLiveTest;
import org.jclouds.vcloud.options.CatalogItemOptions;
import org.jclouds.vcloud.predicates.TaskSuccess;
import org.testng.annotations.Test;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;

@Test(groups = "live", enabled = true, singleThreaded = true, testName = "VAppTemplateApiLiveTest")
public class VAppTemplateApiLiveTest extends BaseVCloudApiLiveTest {
   @Test
   public void testGetVAppTemplate() throws Exception {
      Org org = getVCloudApi().getOrgApi().findOrgNamed(null);
      for (ReferenceType cat : org.getCatalogs().values()) {
         Catalog response = getVCloudApi().getCatalogApi().getCatalog(cat.getHref());
         for (ReferenceType resource : response.values()) {
            if (resource.getType().equals(VCloudMediaType.CATALOGITEM_XML)) {
               CatalogItem item = getVCloudApi().getCatalogApi().getCatalogItem(resource.getHref());
               if (item.getEntity().getType().equals(VCloudMediaType.VAPPTEMPLATE_XML)) {
                  VAppTemplate template = getVCloudApi().getVAppTemplateApi().getVAppTemplate(item.getEntity().getHref());
                  if (template != null) {
                     // the UUID in the href is the only way to actually link templates
                     assertEquals(template.getHref(), item.getEntity().getHref());
                  } else {
                     // null can be no longer available or auth exception
                  }
               }
            }
         }
      }
   }

   @Test
   public void testGetOvfEnvelopeForVAppTemplate() throws Exception {
      Org org = getVCloudApi().getOrgApi().findOrgNamed(null);
      for (ReferenceType cat : org.getCatalogs().values()) {
         Catalog response = getVCloudApi().getCatalogApi().getCatalog(cat.getHref());
         for (ReferenceType resource : response.values()) {
            if (resource.getType().equals(VCloudMediaType.CATALOGITEM_XML)) {
               CatalogItem item = getVCloudApi().getCatalogApi().getCatalogItem(resource.getHref());
               if (item.getEntity().getType().equals(VCloudMediaType.VAPPTEMPLATE_XML)) {
                  getVCloudApi().getVAppTemplateApi().getOvfEnvelopeForVAppTemplate(item.getEntity().getHref());
                  // null can be no longer available or auth exception
               }
            }
         }
      }
   }

   @Test
   public void testFindVAppTemplate() throws Exception {
      Org org = getVCloudApi().getOrgApi().findOrgNamed(null);
      for (ReferenceType cat : org.getCatalogs().values()) {
         Catalog response = getVCloudApi().getCatalogApi().getCatalog(cat.getHref());
         for (ReferenceType resource : response.values()) {
            if (resource.getType().equals(VCloudMediaType.CATALOGITEM_XML)) {
               CatalogItem item = getVCloudApi().getCatalogApi().getCatalogItem(resource.getHref());
               if (item.getEntity().getType().equals(VCloudMediaType.VAPPTEMPLATE_XML)) {
                  VAppTemplate template = getVCloudApi().getVAppTemplateApi().findVAppTemplateInOrgCatalogNamed(
                           org.getName(), response.getName(), item.getEntity().getName());
                  if (template != null) {
                     // the UUID in the href is the only way to actually link templates
                     assertEquals(template.getHref(), item.getEntity().getHref());
                  } else {
                     // null can be no longer available or auth exception
                  }
               }
            }
         }
      }
   }

   @Test
   public void testCaptureVApp() throws Exception {
      String group = prefix + "cap";
      NodeMetadata node = null;
      VAppTemplate vappTemplate = null;
      CatalogItem item = null;
      try {

         node = getOnlyElement(client.createNodesInGroup(group, 1));

         Predicate<URI> taskTester = retry(new TaskSuccess(getVCloudApi()), 600, 5, SECONDS);

         // I have to undeploy first
         Task task = getVCloudApi().getVAppApi().undeployVApp(URI.create(node.getId()));

         // wait up to ten minutes per above
         assert taskTester.apply(task.getHref()) : node;

         VApp vApp = getVCloudApi().getVAppApi().getVApp(URI.create(node.getId()));

         // wait up to ten minutes per above
         assertEquals(vApp.getStatus(), Status.OFF);

         // vdc is equiv to the node's location
         // vapp uri is the same as the node's id
         vappTemplate = getVCloudApi().getVAppTemplateApi().captureVAppAsTemplateInVDC(URI.create(node.getId()),
                  group, URI.create(node.getLocation().getId()));

         assertEquals(vappTemplate.getName(), group);

         task = vappTemplate.getTasks().get(0);

         // wait up to ten minutes per above
         assert taskTester.apply(task.getHref()) : vappTemplate;

         item = getVCloudApi().getCatalogApi().addVAppTemplateOrMediaImageToCatalogAndNameItem(
                  vappTemplate.getHref(),
                  getVCloudApi().getCatalogApi().findCatalogInOrgNamed(null, null).getHref(), "fooname",
                  CatalogItemOptions.Builder.description("description").properties(ImmutableMap.of("foo", "bar")));

         assertEquals(item.getName(), "fooname");
         assertEquals(item.getDescription(), "description");
         assertEquals(item.getProperties(), ImmutableMap.of("foo", "bar"));
         assertEquals(item.getEntity().getName(), "fooname");
         assertEquals(item.getEntity().getHref(), vappTemplate.getHref());
         assertEquals(item.getEntity().getType(), vappTemplate.getType());

      } finally {
         if (item != null)
            getVCloudApi().getCatalogApi().deleteCatalogItem(item.getHref());
         if (vappTemplate != null)
            getVCloudApi().getVAppTemplateApi().deleteVAppTemplate(vappTemplate.getHref());
         if (node != null)
            client.destroyNode(node.getId());
      }
   }
}
