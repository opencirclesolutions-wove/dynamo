/*
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package com.ocs.dynamo.ui;

import jakarta.servlet.ServletContext;

import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.ocs.dynamo.service.impl.BaseSpringServiceLocator;
import com.vaadin.flow.server.VaadinServlet;

public class SpringWebServiceLocator extends BaseSpringServiceLocator {

    /**
     * Lazily loads the context
     */
    protected ApplicationContext loadCtx() {
        if (VaadinServlet.getCurrent() != null) {
            ServletContext sc = VaadinServlet.getCurrent().getServletContext();
            return WebApplicationContextUtils.getWebApplicationContext(sc);
        }
        return null;
    }
}
