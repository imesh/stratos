/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.common.client;

import org.apache.axis2.AxisFault;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.constants.StratosConstants;
import org.apache.stratos.manager.service.stub.StratosManagerServiceApplicationSignUpExceptionException;
import org.apache.stratos.manager.service.stub.StratosManagerServiceArtifactDistributionCoordinatorExceptionException;
import org.apache.stratos.manager.service.stub.StratosManagerServiceDomainMappingExceptionException;
import org.apache.stratos.manager.service.stub.StratosManagerServiceStub;
import org.apache.stratos.manager.service.stub.domain.application.signup.ApplicationSignUp;
import org.apache.stratos.manager.service.stub.domain.application.signup.DomainMapping;

import java.rmi.RemoteException;

/**
 * Stratos manager service client.
 */
public class StratosManagerServiceClient {

    private StratosManagerServiceStub stub;

    private static final Log log = LogFactory.getLog(StratosManagerServiceClient.class);
    private static volatile StratosManagerServiceClient instance;

    private StratosManagerServiceClient(String epr) throws AxisFault {

        String ccSocketTimeout = System.getProperty(StratosConstants.STRATOS_MANAGER_CLIENT_SOCKET_TIMEOUT) == null ?
                StratosConstants.DEFAULT_CLIENT_SOCKET_TIMEOUT :
                System.getProperty(StratosConstants.STRATOS_MANAGER_CLIENT_SOCKET_TIMEOUT);

        String ccConnectionTimeout = System.getProperty(StratosConstants.STRATOS_MANAGER_CLIENT_CONNECTION_TIMEOUT) == null ?
                StratosConstants.DEFAULT_CLIENT_CONNECTION_TIMEOUT :
                System.getProperty(StratosConstants.STRATOS_MANAGER_CLIENT_CONNECTION_TIMEOUT);

        try {
            stub = new StratosManagerServiceStub(epr);
            stub._getServiceClient().getOptions().setProperty(HTTPConstants.SO_TIMEOUT, new Integer(ccSocketTimeout));
            stub._getServiceClient().getOptions().setProperty(HTTPConstants.CONNECTION_TIMEOUT, new Integer(ccConnectionTimeout));

        } catch (AxisFault axisFault) {
            String msg = "Could not initialize stratos manager service client";
            log.error(msg, axisFault);
            throw new AxisFault(msg, axisFault);
        }
    }

    public static StratosManagerServiceClient getInstance() throws AxisFault {
        if (instance == null) {
            synchronized (StratosManagerServiceClient.class) {
                if (instance == null) {
                    String cloudControllerServiceUrl = System.getProperty(StratosConstants.STRATOS_MANAGER_SERVICE_URL);
                    if (StringUtils.isBlank(cloudControllerServiceUrl)) {
                        throw new RuntimeException(String.format("System property not found: %s",
                                StratosConstants.STRATOS_MANAGER_SERVICE_URL));
                    }
                    instance = new StratosManagerServiceClient(cloudControllerServiceUrl);
                }
            }
        }
        return instance;
    }

    /**
     * Add application signup
     *
     * @param applicationSignUp
     */
    public void addApplicationSignUp(ApplicationSignUp applicationSignUp) throws StratosManagerServiceApplicationSignUpExceptionException, RemoteException {
        stub.addApplicationSignUp(applicationSignUp);
    }

    /**
     * Remove application signup.
     *
     * @param applicationId
     * @param tenantId
     */
    public void removeApplicationSignUp(String applicationId, int tenantId) throws StratosManagerServiceApplicationSignUpExceptionException, RemoteException {
        stub.removeApplicationSignUp(applicationId, tenantId);
    }

    /**
     * Get application signup.
     *
     * @param applicationId
     * @param tenantId
     * @return
     */
    public ApplicationSignUp getApplicationSignUp(String applicationId, int tenantId) throws StratosManagerServiceApplicationSignUpExceptionException, RemoteException {
        return stub.getApplicationSignUp(applicationId, tenantId);
    }

    /**
     * Get application signups.
     *
     * @return
     */
    public ApplicationSignUp[] getApplicationSignUps(String applicationId) throws StratosManagerServiceApplicationSignUpExceptionException, RemoteException {
        return stub.getApplicationSignUps(applicationId);
    }

    /**
     * Notify artifact updated event for application signup.
     *
     * @param applicationId
     * @param tenantId
     * @throws StratosManagerServiceArtifactDistributionCoordinatorExceptionException
     * @throws RemoteException
     */
    public void notifyArtifactUpdatedEventForSignUp(String applicationId, int tenantId) throws StratosManagerServiceArtifactDistributionCoordinatorExceptionException, RemoteException {
        stub.notifyArtifactUpdatedEventForSignUp(applicationId, tenantId);
    }

    /**
     * Notify artifact updated event for artifact repository.
     *
     * @param repoUrl
     * @throws StratosManagerServiceArtifactDistributionCoordinatorExceptionException
     * @throws RemoteException
     */
    public void notifyArtifactUpdatedEventForRepository(String repoUrl) throws StratosManagerServiceArtifactDistributionCoordinatorExceptionException, RemoteException {
        stub.notifyArtifactUpdatedEventForRepository(repoUrl);
    }

    public void addDomainMapping(DomainMapping domainMapping) throws RemoteException, StratosManagerServiceDomainMappingExceptionException {
        stub.addDomainMapping(domainMapping);
    }

    public void removeDomainMapping(String applicationId, int tenantId, String domainName) throws RemoteException, StratosManagerServiceDomainMappingExceptionException {
        stub.removeDomainMapping(applicationId, tenantId, domainName);
    }

    public DomainMapping[] getDomainMappings(String applicationId, int tenantId) throws RemoteException, StratosManagerServiceDomainMappingExceptionException {
        return stub.getDomainMappings(applicationId, tenantId);
    }
}
