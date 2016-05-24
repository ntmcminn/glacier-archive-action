/**
 * Extended from onActionSimpleRepoAction on actions.js to add waiting popup
 */
(function() {
	YAHOO.Bubbling.fire("registerAction", {
		actionName : "onGlacierArchiveAction",
		fn: function onGlacierArchiveAction(record, owner) {
			
			this.widgets.waitDialog = Alfresco.util.PopupManager.displayMessage({
				text : this.msg("message.glacier-archive.working"),
				spanClass : "wait",
				displayTime : 0
			});
			
			// Get action params
	         var params = this.getAction(record, owner).params,
	            displayName = record.displayName,
	            namedParams = ["function", "action", "success", "successMessage", "failure", "failureMessage"],
	            repoActionParams = {};

	         for (var name in params)
	         {
	            if (params.hasOwnProperty(name) && !Alfresco.util.arrayContains(namedParams, name))
	            {
	               repoActionParams[name] = params[name];
	            }
	         }

	         // Deactivate action
	         var ownerTitle = owner.title;
	         owner.title = owner.title + "_deactivated";

	         // Prepare genericAction config
	         var config =
	         {
	            success:
	            {
	               event:
	               {
	                  name: "metadataRefresh",
	                  obj: record
	               }
	            },
	            failure:
	            {
	               message: this.msg(params.failureMessage, displayName),
	               fn: function showAction()
	               {
	                  owner.title = ownerTitle;
	               },
	               scope: this
	            },
	            webscript:
	            {
	               method: Alfresco.util.Ajax.POST,
	               stem: Alfresco.constants.PROXY_URI + "api/",
	               name: "actionQueue"
	            },
	            config:
	            {
	               requestContentType: Alfresco.util.Ajax.JSON,
	               dataObj:
	               {
	                  actionedUponNode: record.nodeRef,
	                  actionDefinitionName: params.action,
	                  parameterValues: repoActionParams
	               }
	            }
	         };

	         // Add configured success callbacks and messages if provided
	         if (YAHOO.lang.isFunction(this[params.success]))
	         {
	            config.success.callback =
	            {
	               fn: this[params.success],
	               obj: record,
	               scope: this
	            };
	         }
	         if (params.successMessage)
	         {
	            config.success.message = this.msg(params.successMessage, displayName);
	         }

	         // Acd configured failure callback and message if provided
	         if (YAHOO.lang.isFunction(this[params.failure]))
	         {
	            config.failure.callback =
	            {
	               fn: this[params.failure],
	               obj: record,
	               scope: this
	            };
	         }
	         if (params.failureMessage)
	         {
	            config.failure.message = this.msg(params.failureMessage, displayName);
	         }

	         // Execute the repo action
	         this.modules.actions.genericAction(config);
			
	      }		
    });			
	
	YAHOO.Bubbling.fire("registerAction", {
		actionName : "onGlacierInitiateRetrievalAction",
		fn: function onGlacierInitiateRetrievalAction(record, owner) {
			
			this.widgets.waitDialog = Alfresco.util.PopupManager.displayMessage({
				text : this.msg("message.glacier-retrieval-initiate.working"),
				spanClass : "wait",
				displayTime : 0
			});
			
			// Get action params
	         var params = this.getAction(record, owner).params,
	            displayName = record.displayName,
	            namedParams = ["function", "action", "success", "successMessage", "failure", "failureMessage"],
	            repoActionParams = {};

	         for (var name in params)
	         {
	            if (params.hasOwnProperty(name) && !Alfresco.util.arrayContains(namedParams, name))
	            {
	               repoActionParams[name] = params[name];
	            }
	         }

	         // Deactivate action
	         var ownerTitle = owner.title;
	         owner.title = owner.title + "_deactivated";

	         // Prepare genericAction config
	         var config =
	         {
	            success:
	            {
	               event:
	               {
	                  name: "metadataRefresh",
	                  obj: record
	               }
	            },
	            failure:
	            {
	               message: this.msg(params.failureMessage, displayName),
	               fn: function showAction()
	               {
	                  owner.title = ownerTitle;
	               },
	               scope: this
	            },
	            webscript:
	            {
	               method: Alfresco.util.Ajax.POST,
	               stem: Alfresco.constants.PROXY_URI + "api/",
	               name: "actionQueue"
	            },
	            config:
	            {
	               requestContentType: Alfresco.util.Ajax.JSON,
	               dataObj:
	               {
	                  actionedUponNode: record.nodeRef,
	                  actionDefinitionName: params.action,
	                  parameterValues: repoActionParams
	               }
	            }
	         };

	         // Add configured success callbacks and messages if provided
	         if (YAHOO.lang.isFunction(this[params.success]))
	         {
	            config.success.callback =
	            {
	               fn: this[params.success],
	               obj: record,
	               scope: this
	            };
	         }
	         if (params.successMessage)
	         {
	            config.success.message = this.msg(params.successMessage, displayName);
	         }

	         // Acd configured failure callback and message if provided
	         if (YAHOO.lang.isFunction(this[params.failure]))
	         {
	            config.failure.callback =
	            {
	               fn: this[params.failure],
	               obj: record,
	               scope: this
	            };
	         }
	         if (params.failureMessage)
	         {
	            config.failure.message = this.msg(params.failureMessage, displayName);
	         }

	         // Execute the repo action
	         this.modules.actions.genericAction(config);
			
	      }		
    });
	
	YAHOO.Bubbling.fire("registerAction", {
		actionName : "onGlacierRetrievalAction",
		fn: function onGlacierRetrievalAction(record, owner) {
			
			this.widgets.waitDialog = Alfresco.util.PopupManager.displayMessage({
				text : this.msg("message.glacier-retrieval.working"),
				spanClass : "wait",
				displayTime : 0
			});
			
			// Get action params
	         var params = this.getAction(record, owner).params,
	            displayName = record.displayName,
	            namedParams = ["function", "action", "success", "successMessage", "failure", "failureMessage"],
	            repoActionParams = {};

	         for (var name in params)
	         {
	            if (params.hasOwnProperty(name) && !Alfresco.util.arrayContains(namedParams, name))
	            {
	               repoActionParams[name] = params[name];
	            }
	         }

	         // Deactivate action
	         var ownerTitle = owner.title;
	         owner.title = owner.title + "_deactivated";

	         // Prepare genericAction config
	         var config =
	         {
	            success:
	            {
	               event:
	               {
	                  name: "metadataRefresh",
	                  obj: record
	               }
	            },
	            failure:
	            {
	               message: this.msg(params.failureMessage, displayName),
	               fn: function showAction()
	               {
	                  owner.title = ownerTitle;
	               },
	               scope: this
	            },
	            webscript:
	            {
	               method: Alfresco.util.Ajax.POST,
	               stem: Alfresco.constants.PROXY_URI + "api/",
	               name: "actionQueue"
	            },
	            config:
	            {
	               requestContentType: Alfresco.util.Ajax.JSON,
	               dataObj:
	               {
	                  actionedUponNode: record.nodeRef,
	                  actionDefinitionName: params.action,
	                  parameterValues: repoActionParams
	               }
	            }
	         };

	         // Add configured success callbacks and messages if provided
	         if (YAHOO.lang.isFunction(this[params.success]))
	         {
	            config.success.callback =
	            {
	               fn: this[params.success],
	               obj: record,
	               scope: this
	            };
	         }
	         if (params.successMessage)
	         {
	            config.success.message = this.msg(params.successMessage, displayName);
	         }

	         // Acd configured failure callback and message if provided
	         if (YAHOO.lang.isFunction(this[params.failure]))
	         {
	            config.failure.callback =
	            {
	               fn: this[params.failure],
	               obj: record,
	               scope: this
	            };
	         }
	         if (params.failureMessage)
	         {
	            config.failure.message = this.msg(params.failureMessage, displayName);
	         }

	         // Execute the repo action
	         this.modules.actions.genericAction(config);
			
	      }		
    });
})();
