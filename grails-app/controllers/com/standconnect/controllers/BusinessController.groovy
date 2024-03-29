package com.standconnect.controllers



import static org.springframework.http.HttpStatus.*
import grails.plugin.springsecurity.annotation.Secured
import grails.transaction.Transactional

import com.standconnect.domain.Business

@Transactional(readOnly = true)
class BusinessController {

    static allowedMethods = [save: "POST", update: "POST", delete: "DELETE"]

	def afterInterceptor = { model, modelAndView ->
		model.controller = "business"
		model.view = modelAndView?.viewName
	}
	
	def springSecurityService
	
	@Secured(["IS_AUTHENTICATED_REMEMBERED"])
	def userBusinesses(Integer max) {
		params.max = Math.min(max ?: 10, 100)
		respond Business.createCriteria().list(params) {
			eq('businessUser', springSecurityService.getCurrentUser())
		}
	}
	
//	@Secured(["IS_AUTHENTICATED_REMEMBERED"])
    def index(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        respond Business.list(params), model:[businessInstanceCount: Business.count()]
    }

//	@Secured(["IS_AUTHENTICATED_REMEMBERED"])
    def show(Business businessInstance) {
        respond businessInstance
    }

	@Secured(["ROLE_ADMIN","ROLE_BUSINESSUSER"])
    def create() {
        respond new Business(params)
    }

    @Transactional
	@Secured(["ROLE_ADMIN","ROLE_BUSINESSUSER"])
    def save(Business businessInstance) {
        if (businessInstance == null) {
            notFound()
            return
        }

		businessInstance.businessUser = springSecurityService.getCurrentUser()
		
        if (!businessInstance.save()) {
            respond businessInstance.errors, view:'create'
            return
        }

        businessInstance.save flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.created.message', args: [message(code: 'business.label', default: 'Business'), businessInstance.id])
                redirect businessInstance
            }
            '*' { respond businessInstance, [status: CREATED] }
        }
    }

	@Secured(["ROLE_ADMIN","ROLE_BUSINESSUSER"])
    def edit(Business businessInstance) {
		if(!hasPermission(businessInstance)) {
			render status: 403
			return
		}
		
        respond businessInstance
    }

    @Transactional
	@Secured(["ROLE_ADMIN","ROLE_BUSINESSUSER"])
    def update(Business businessInstance) {
		if(!hasPermission(businessInstance)) {
			render status: 403
			return
		}
		
        if (businessInstance == null) {
            notFound()
            return
        }

        if (businessInstance.hasErrors()) {
            respond businessInstance.errors, view:'edit'
            return
        }

        businessInstance.save flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.updated.message', args: [message(code: 'Business.label', default: 'Business'), businessInstance.id])
                redirect businessInstance
            }
            '*'{ respond businessInstance, [status: OK] }
        }
    }

    @Transactional
	@Secured(["ROLE_ADMIN","ROLE_BUSINESSUSER"])
    def delete(Business businessInstance) {
		if(!hasPermission(businessInstance)) {
			render status: 403
			return
		}
		
        if (businessInstance == null) {
            notFound()
            return
        }

        businessInstance.delete flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.deleted.message', args: [message(code: 'Business.label', default: 'Business'), businessInstance.id])
                redirect action:"index", method:"GET"
            }
            '*'{ render status: NO_CONTENT }
        }
    }

	def getBusinessImage() {
		def business = Business.get(Long.parseLong(params.id, 10))
		
		if(!business) {
			notFound()
			return
		}
		
		byte[] image = business.image
		response.outputStream << image
	}
	
    protected void notFound() {
        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.not.found.message', args: [message(code: 'business.label', default: 'Business'), params.id])
                redirect action: "index", method: "GET"
            }
            '*'{ render status: NOT_FOUND }
        }
    }
	
	private hasPermission(businessInstance) {
		def currentUser = springSecurityService.getCurrentUser()
		
		if("ROLE_ADMIN" in currentUser?.authorities*.authority || businessInstance?.businessUser == currentUser) {
			return true
		}
		else {
			return false
		}
	}
}
