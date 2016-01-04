package controllers.config

import org.coursera.autoschema.annotations._

case class GroupConfiguration (@Term.Hide groupid: String, @Term.Description("This is the API key for your team.") @Term.Title("GitHub API Key") gitHubAPIKey: String)
