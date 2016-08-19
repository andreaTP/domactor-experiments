package eu.unicredit

import scala.scalajs.js
import org.scalajs.dom.document.{getElementsByClassName => getElem}

import akka.actor._

import scalatags.JsDom._
import scalatags.JsDom.all._
import org.scalajs.dom

object Main extends js.JSApp {

  case class AddToDoElem(txt: String)
  case object ToDoElemRemoved

  case class ToDoMVC() extends DomActor {
    override val domElement = Some(getElem("todoapp")(0))

    def sendOrAdd(text: String) = {
      context.child("mainsection") match {
        case Some(ms) => ms ! AddToDoElem(text)
        case _ =>
          context.actorOf(Props(MainSectionActor(text)), "mainsection")
      }
    }

    def template = {
      val inputBox: dom.html.Input =
        input(
          cls := "new-todo",
          attr("placeholder") := "What needs to be done?",
          onkeydown := {(event: js.Dynamic) =>
            if(event.keyCode == 13) {
              event.preventDefault()
              val text = ""+event.target.value
              event.target.value = ""
              sendOrAdd(text)
            }
          }
        ).render

      div(attr("data-reactroot") := "")(
        h1("todos"),
        header(cls :="header")(
          inputBox
        )
      )
    }
  }

  case class MainSectionActor(firstElem: String) extends DomActor {
    def template =
      tag("section")(cls := "main")(
        input(`type` := "checkbox", cls := "toggle-all", value := "on")
      )

    override def operative = {
      val listActor = context.actorOf(Props(ListActor(firstElem)))

      context.actorOf(Props(FooterActor()), "footer")

      domManagement orElse {
        case any => listActor ! any
      }
    }
  }

  case object Add
  case object Sub

  case class FooterActor() extends DomActorWithParams[Int] {
    val initValue = 0

    def template(n: Int) =
      tag("section")(
        footer(cls := "footer")(
          span(cls := "todo-count")(
            strong(n),
            " items left				"
          )
        )
      )

    override def operative = count(initValue)

    def count(n: Int): Receive = {
      self ! UpdateValue(n)

      domManagement orElse {
        case Add => context.become(count(n+1))
        case Sub => context.become(count(n-1))
      }
    }
  }

  case class ListActor(firstElem: String) extends DomActor {

    def template =
        ul(cls := "todo-list")

    def footer() =
      context.actorSelection(context.parent.path + "/footer")

    override def operative = {
      context.actorOf(Props(ToDoElem(firstElem, footer)))
      footer ! Add

      domManagement orElse {
        case AddToDoElem(txt) =>
          context.actorOf(Props(ToDoElem(txt, footer)))

          footer ! Add
        case ToDoElemRemoved =>
          if (context.children.isEmpty) {
            context.parent ! PoisonPill
          }
      }
    }

  }

  case class ToDoElem(
      txt: String,
      footer: () => ActorSelection
    ) extends DomActorWithParams[Boolean] {
    override val domElement = Some(getElem("todo-list")(0))

    val initValue = false

    def template(checked: Boolean) = {
      println("template triggered "+checked)
      li(
        div(cls := "view")(
          input(`type` :="checkbox", cls := "toggle",
            attr("value") := {if (checked) "on" else "off"},
            onclick := {() =>
              println("updating value!"+(!checked))
              self ! UpdateValue(!checked)
              if (checked) footer() ! Sub
              else footer() ! Add
            }
          ),
          label(txt),
          button(cls := "destroy", onclick := {() =>
            self ! PoisonPill
          })
        ),
        input(value := txt, cls := "edit")
      )
    }

    override def postStop() = {
      super.postStop()
      footer() ! Sub
      context.parent ! ToDoElemRemoved
    }
  }

/*
<div data-reactroot=""><h1>todos</h1>
<header class="header">
<input placeholder="What needs to be done?" class="new-todo">
</header>
<section class="main">
<input type="checkbox" class="toggle-all" value="on">
<ul class="todo-list">
  <li>
    <div class="view"><input type="checkbox" class="toggle" value="on">
    <label>one</label>
    <button class="destroy">
    </button></div><input value="one" class="edit"></li></ul>
    </section>
    <footer class="footer"><span class="todo-count"><strong>1</strong><!-- react-text: 50 --> item left<!-- /react-text --></span><ul class="filters"><li><a href="http://todomvc.com/examples/scalajs-react/#/" class="selected">All</a></li><li><a href="http://todomvc.com/examples/scalajs-react/#/active">Active</a></li><li><a href="http://todomvc.com/examples/scalajs-react/#/completed">Completed</a></li></ul><button class="clear-completed" style="visibility: hidden;">Clear completed</button></footer></div>
*/
  def main() = {
    implicit lazy val system = ActorSystem("todo")

    system.actorOf(Props(ToDoMVC()))
  }

}
