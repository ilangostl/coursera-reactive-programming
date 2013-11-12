package simulations

import common._

class Wire {
  private var sigVal = false
  private var actions: List[Simulator#Action] = List()

  def getSignal: Boolean = sigVal
  
  def setSignal(s: Boolean) {
    if (s != sigVal) {
      sigVal = s
      actions.foreach(action => action())
    }
  }

  def addAction(a: Simulator#Action) {
    actions = a :: actions
    a()
  }
}

abstract class CircuitSimulator extends Simulator {

  val InverterDelay: Int
  val AndGateDelay: Int
  val OrGateDelay: Int

  def probe(name: String, wire: Wire) {
    wire addAction {
      () => afterDelay(0) {
        println(
          "  " + currentTime + ": " + name + " -> " +  wire.getSignal)
      }
    }
  }

  def inverter(input: Wire, output: Wire) {
    def invertAction() {
      val inputSig = input.getSignal
      afterDelay(InverterDelay) { output.setSignal(!inputSig) }
    }
    input addAction invertAction
  }

  def andGate(a1: Wire, a2: Wire, output: Wire) {
    def andAction() {
      val a1Sig = a1.getSignal
      val a2Sig = a2.getSignal
      afterDelay(AndGateDelay) { output.setSignal(a1Sig & a2Sig) }
    }
    a1 addAction andAction
    a2 addAction andAction
  }

  //
  // to complete with orGates and demux...
  //

  def orGate(a1: Wire, a2: Wire, output: Wire) {
    def orAction() {
      val a1Sig = a1.getSignal
      val a2Sig = a2.getSignal
      afterDelay(OrGateDelay) { output.setSignal(a1Sig | a2Sig) }
    }
    a1.addAction(orAction)
    a2.addAction(orAction)
  }
  
  def orGate2(a1: Wire, a2: Wire, output: Wire) {
    def orAction2() {
      val a1Neg, a2Neg, andResult = new Wire
      inverter(a1, a1Neg)
      inverter(a2, a2Neg)
      andGate(a1Neg, a2Neg, andResult)
      inverter(andResult, output)
    }
    a1.addAction(orAction2)
    a2.addAction(orAction2)
  }

  def demux(in: Wire, c: List[Wire], out: List[Wire]) {
    def demuxRecursive(control: List[Wire], output: List[Wire], wireSoFar: Wire) {
      control match {
        case Nil =>
          assert(output.length == 1)
          andGate(wireSoFar, in, output.head)

        case c1 :: cs =>
          val parts = output.grouped(output.length / 2)

          val positiveHalf = parts.next()
          val positiveWire = new Wire
          andGate(c1, wireSoFar, positiveWire)
          demuxRecursive(cs, positiveHalf, positiveWire)

          val negativeHalf = parts.next()
          val negativeCtrl = new Wire
          inverter(c1, negativeCtrl)
          val negativeWire = new Wire
          andGate(negativeCtrl, wireSoFar, negativeWire)
          demuxRecursive(cs, negativeHalf, negativeWire)
      }
    }

    def demuxAction() {
      val parts = out.grouped(out.length / 2)

      val positiveHalf = parts.next()
      demuxRecursive(c.tail, positiveHalf, c.head)

      val negativeHalf = parts.next()
      val notControl = new Wire
      inverter(c.head, notControl)
      demuxRecursive(c.tail, negativeHalf, notControl)
    }

    in.addAction(demuxAction)
    c.foreach(_.addAction(demuxAction))
  }

}

object Circuit extends CircuitSimulator {
  val InverterDelay = 1
  val AndGateDelay = 3
  val OrGateDelay = 5

  def andGateExample {
    val in1, in2, out = new Wire
    andGate(in1, in2, out)
    probe("in1", in1)
    probe("in2", in2)
    probe("out", out)
    in1.setSignal(false)
    in2.setSignal(false)
    run

    in1.setSignal(true)
    run

    in2.setSignal(true)
    run
  }

  //
  // to complete with orGateExample and demuxExample...
  //
}

object CircuitMain extends App {
  // You can write tests either here, or better in the test class CircuitSuite.
  Circuit.andGateExample
}
