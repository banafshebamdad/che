/*******************************************************************************
 * Copyright (c) 2012-2017 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.api.user.server;

import org.eclipse.che.api.core.rest.ServiceContext;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.core.util.LinksHelper;
import org.eclipse.che.api.user.shared.dto.UserDto;

import javax.inject.Singleton;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.UriBuilder;
import java.util.ArrayList;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.che.api.user.server.Constants.LINK_REL_CURRENT_USER;
import static org.eclipse.che.api.user.server.Constants.LINK_REL_CURRENT_USER_PASSWORD;
import static org.eclipse.che.api.user.server.Constants.LINK_REL_CURRENT_USER_SETTINGS;
import static org.eclipse.che.api.user.server.Constants.LINK_REL_PREFERENCES;
import static org.eclipse.che.api.user.server.Constants.LINK_REL_PROFILE;
import static org.eclipse.che.api.user.server.Constants.LINK_REL_SELF;

/**
 * Helps to inject {@link UserService} related links.
 *
 * @author Anatoliy Bazko
 * @author Yevhenii Voevodin
 */
@Singleton
public class UserLinksInjector {

    public UserDto injectLinks(UserDto userDto, ServiceContext serviceContext) {
        final UriBuilder uriBuilder = serviceContext.getBaseUriBuilder();
        final List<Link> links = new ArrayList<>(6);
        links.add(LinksHelper.createLink(HttpMethod.GET,
                                         uriBuilder.clone()
                                                   .path(UserService.class)
                                                   .path(UserService.class, "getById")
                                                   .build(userDto.getId())
                                                   .toString(),
                                         null,
                                         APPLICATION_JSON,
                                         LINK_REL_SELF));
        links.add(LinksHelper.createLink(HttpMethod.GET,
                                         uriBuilder.clone()
                                                   .path(UserService.class)
                                                   .path(UserService.class, "getCurrent")
                                                   .build()
                                                   .toString(),
                                         null,
                                         APPLICATION_JSON,
                                         LINK_REL_CURRENT_USER));
        links.add(LinksHelper.createLink(HttpMethod.POST,
                                         uriBuilder.clone()
                                                   .path(UserService.class)
                                                   .path(UserService.class, "updatePassword")
                                                   .build()
                                                   .toString(),
                                         APPLICATION_FORM_URLENCODED,
                                         null,
                                         LINK_REL_CURRENT_USER_PASSWORD));
        links.add(LinksHelper.createLink(HttpMethod.GET,
                                         uriBuilder.clone()
                                                   .path(ProfileService.class)
                                                   .path(ProfileService.class, "getById")
                                                   .build(userDto.getId())
                                                   .toString(),
                                         null,
                                         APPLICATION_JSON,
                                         LINK_REL_PROFILE));
        links.add(LinksHelper.createLink(HttpMethod.GET,
                                         uriBuilder.clone()
                                                   .path(UserService.class)
                                                   .path(UserService.class, "getSettings")
                                                   .build()
                                                   .toString(),
                                         null,
                                         APPLICATION_JSON,
                                         LINK_REL_CURRENT_USER_SETTINGS));
        links.add(LinksHelper.createLink(HttpMethod.GET,
                                         uriBuilder.clone().path(PreferencesService.class)
                                                   .path(PreferencesService.class, "find")
                                                   .build()
                                                   .toString(),
                                         null,
                                         APPLICATION_JSON,
                                         LINK_REL_PREFERENCES));
        return userDto.withLinks(links);
    }
}
