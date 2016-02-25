package controllers

import org.coursera.autoschema.annotations._

case class GroupConfiguration (@Term.Hide groupid: String, @Term.Description("This is the API key for your team.") @Term.Title("GitHub API Key") gitHubAPIKey: String,
                               @Term.Description("Optional GitHub Username for Authentication") @Term.Title("User") username: String,
                               @Term.Description("Optional GitHub Password for Authentication") @Term.Title("Password") password: String)

case class BjondRegistration(id: String, url: String)