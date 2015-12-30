package controllers.config

import org.coursera.autoschema.annotations._

class ServiceConfiguration () {
	case class GroupConfiguration (@Term.Description("This is the API key for your team.") @Term.Title("GitHub API Key") gitHubAPIKey: String, @Term.Description("Just playing- this does nothing.") @Term.Title("Random") Description: String)
}