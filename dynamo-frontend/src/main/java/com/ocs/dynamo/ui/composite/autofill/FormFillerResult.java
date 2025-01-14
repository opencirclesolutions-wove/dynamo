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
package com.ocs.dynamo.ui.composite.autofill;

import java.io.Serializable;

/**
 * FormFiller result after a {@link FormFiller#fill} call.
 * Provides information about the request for the AI module
 * and the response returned from the same AI modue.
 *
 * @author Vaadin Ltd.
 */
public class FormFillerResult implements Serializable {

    /**
     * Prompt request to the AI module
     */
    String request;


    /**
     * Prompt response from to the AI module
     */
    String response;

    /**
     *
     * @param request Prompt request to the AI module
     * @param response Prompt response from to the AI module
     */
    public FormFillerResult(String request, String response) {
        this.request = request;
        this.response = response;
    }

    public String getRequest() {
        return request;
    }

    public void setRequest(String request) {
        this.request = request;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }
}
