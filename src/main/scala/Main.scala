import Arg.*
import cats.effect.ExitCode
import com.wavesplatform.transactions.account.{ Address, PrivateKey }
import com.wavesplatform.transactions.common.Amount
import com.wavesplatform.transactions.invocation.{ Function, StringArg }
import com.wavesplatform.transactions.InvokeScriptTransaction
import com.wavesplatform.wavesj.{ Node, Profile }
import concurrent.duration.*
import monix.eval.{ Task, TaskApp }
import monix.execution.Scheduler.Implicits.global
import monix.reactive.Observable
import scala.util.Failure
import scala.util.Try

import java.lang.{ IllegalArgumentException as IE }

enum Arg:
  case EncodedPrivKey, Beneficiary, Interval

case class Params(privateKey: PrivateKey, beneficiary: Address, interval: Int)

object Main extends TaskApp:

  val dapp = "3P9vKqQKjUdmpXAfiWau8krREYAY1Xr69pE"
  val fee  = 500000L

  def run(args: List[String]): Task[ExitCode] =
    Task.pure(args)
      .flatMap(argCheckTask)
      .flatMap(argTransformTask)
      .flatMap(param => printParamsTask(param) >> invokerTask(param).as(ExitCode.Success))
      .onErrorHandleWith(t => Task(System.err.println(s"ERROR: ${t.getMessage}")).as(ExitCode(2)))

  val invokerTask: Params => Task[List[Unit]] =
    params =>
      (if params.interval == 0 then Observable.pure(0) else Observable.interval(params.interval.hour))
        .mapEval(_ =>
          Task {
            val privateKey = params.privateKey
            val node       = Node(Profile.MAINNET)
            val amount     = node.getBalance(privateKey.address) - fee
            val tx         = distributeAwardTx(Amount(amount), privateKey, params.beneficiary)

            node.broadcast(tx)
          }.attempt
        )
        .collect {
          case Right(tx) => println(s"TX_ID: ${tx.id}")
          case Left(e)   => System.err.println(s"Error broadcasting TX: ${e.getMessage}")
        }
        .toListL

  val printParamsTask: Params => Task[Unit] =
    params =>
      Task {
        println(s"""
---- STARTING DISTRIBUTER ----
NODE: ${params.privateKey.address.encoded}
BENEFICIARY ADDRESS: ${params.beneficiary.encoded}
INTERVAL: ${params.interval} hours
------------------------------
      """)
      }

  val argCheckTask: List[String] => Task[Map[Arg, String]] =
    args =>
      Task(args.sliding(2, 2))
        .map(arg =>
          for (arg <- args.sliding(2, 2))
            yield arg match
              case List("-p", arg: String) => (EncodedPrivKey -> arg)
              case List("-b", arg: String) => (Beneficiary    -> arg)
              case List("-i", arg: String) => (Interval       -> arg)
              case _                       => throw IE("Parameters not properly set")
        ).map(_.toMap)

  val argTransformTask: Map[Arg, String] => Task[Params] =
    argMap =>
      Task.pure(argMap)
        .map(argMap => Arg.values.find(m => !argMap.contains(m)))
        .flatMap {
          case Some(a) => Task.raiseError(IE(s"${a} parameter missing"))
          case None    => Task.parMap3(
              Task(argMap.get(EncodedPrivKey))
                .map(_.toList.head)
                .flatMap(pkey =>
                  Task(PrivateKey.as(pkey)).onErrorHandleWith(_ =>
                    Task.raiseError(IE("Not a base 58 encoded privatekey"))
                  )
                ),
              Task(argMap.get(Beneficiary))
                .map(_.toList.head)
                .flatMap(address =>
                  Task(Address.as(address)).onErrorHandleWith(_ =>
                    Task.raiseError(IE("Beneficiary address invalid"))
                  )
                ),
              Task(argMap.get(Interval))
                .map(_.toList.head)
                .flatMap(i =>
                  Task(i.toInt).onErrorHandleWith(_ => Task.raiseError(IE("Incorrect time interval")))
                )
            )(Params.apply)
        }

  def distributeAwardTx(amount: Amount, privateKey: PrivateKey, recipient: Address): InvokeScriptTransaction =
    val function = Function("distributeMinerReward", StringArg(recipient.encoded))

    InvokeScriptTransaction
      .builder(Address(dapp), function)
      .payments(amount)
      .fee(fee)
      .getSignedWith(privateKey)