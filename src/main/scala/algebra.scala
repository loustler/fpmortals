// Copyright: 2017 - 2018 Sam Halliday
// License: http://www.gnu.org/licenses/gpl-3.0.en.html

package fommil
package algebra

import prelude._, Z._

trait Drone[F[_]] {
  def getBacklog: F[Int]
  def getAgents: F[Int]
}

final case class MachineNode(id: String)

trait Machines[F[_]] {
  def getTime: F[Instant]
  def getManaged: F[NonEmptyList[MachineNode]]
  def getAlive: F[Map[MachineNode, Instant]]
  def start(node: MachineNode): F[Unit]
  def stop(node: MachineNode): F[Unit]
}

// everything below this line is boilerplate that should be generated by a
// plugin. Watch out for scalaz-boilerplate
object Drone {

  def liftM[F[_]: Monad, G[_[_], _]: MonadTrans](f: Drone[F]): Drone[G[F, ?]] =
    new Drone[G[F, ?]] {
      def getBacklog: G[F, Int] = f.getBacklog.liftM[G]
      def getAgents: G[F, Int]  = f.getAgents.liftM[G]
    }

  def liftIO[F[_]: MonadIO](io: Drone[IO]): Drone[F] = new Drone[F] {
    def getBacklog: F[Int] = io.getBacklog.liftIO[F]
    def getAgents: F[Int]  = io.getAgents.liftIO[F]
  }

  sealed abstract class Ast[A]
  final case class GetBacklog() extends Ast[Int]
  final case class GetAgents()  extends Ast[Int]

  def liftF[F[_]](implicit I: Ast :<: F): Drone[Free[F, ?]] =
    new Drone[Free[F, ?]] {
      def getBacklog: Free[F, Int] = Free.liftF(I.inj(GetBacklog()))
      def getAgents: Free[F, Int]  = Free.liftF(I.inj(GetAgents()))
    }

  def interpreter[F[_]](f: Drone[F]): Ast ~> F = λ[Ast ~> F] {
    case GetBacklog() => f.getBacklog
    case GetAgents()  => f.getAgents
  }

}

object Machines {
  def liftM[F[_]: Monad, G[_[_], _]: MonadTrans](
    f: Machines[F]
  ): Machines[G[F, ?]] =
    new Machines[G[F, ?]] {
      def getTime: G[F, Instant]                      = f.getTime.liftM[G]
      def getManaged: G[F, NonEmptyList[MachineNode]] = f.getManaged.liftM[G]
      def getAlive: G[F, Map[MachineNode, Instant]]   = f.getAlive.liftM[G]
      def start(node: MachineNode): G[F, Unit]        = f.start(node).liftM[G]
      def stop(node: MachineNode): G[F, Unit]         = f.stop(node).liftM[G]
    }

  def liftIO[F[_]: MonadIO](io: Machines[IO]): Machines[F] = new Machines[F] {
    def getTime: F[Instant]                      = io.getTime.liftIO[F]
    def getManaged: F[NonEmptyList[MachineNode]] = io.getManaged.liftIO[F]
    def getAlive: F[Map[MachineNode, Instant]]   = io.getAlive.liftIO[F]
    def start(node: MachineNode): F[Unit]        = io.start(node).liftIO[F]
    def stop(node: MachineNode): F[Unit]         = io.stop(node).liftIO[F]
  }

  sealed abstract class Ast[A]
  final case class GetTime()                extends Ast[Instant]
  final case class GetManaged()             extends Ast[NonEmptyList[MachineNode]]
  final case class GetAlive()               extends Ast[Map[MachineNode, Instant]]
  final case class Start(node: MachineNode) extends Ast[Unit]
  final case class Stop(node: MachineNode)  extends Ast[Unit]

  def liftF[F[_]](implicit I: Ast :<: F): Machines[Free[F, ?]] =
    new Machines[Free[F, ?]] {
      def getTime: Free[F, Instant] = Free.liftF(I.inj(GetTime()))
      def getManaged: Free[F, NonEmptyList[MachineNode]] =
        Free.liftF(I.inj(GetManaged()))
      def getAlive: Free[F, Map[MachineNode, Instant]] =
        Free.liftF(I.inj(GetAlive()))
      def start(node: MachineNode): Free[F, Unit] =
        Free.liftF(I.inj(Start(node)))
      def stop(node: MachineNode): Free[F, Unit] = Free.liftF(I.inj(Stop(node)))
    }

  def interpreter[F[_]](f: Machines[F]): Ast ~> F = λ[Ast ~> F] {
    case GetTime()    => f.getTime
    case GetManaged() => f.getManaged
    case GetAlive()   => f.getAlive
    case Start(node)  => f.start(node)
    case Stop(node)   => f.stop(node)
  }

}
