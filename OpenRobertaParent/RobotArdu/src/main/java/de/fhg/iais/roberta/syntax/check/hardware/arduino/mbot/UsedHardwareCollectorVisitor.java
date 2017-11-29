package de.fhg.iais.roberta.syntax.check.hardware.arduino.mbot;

import java.util.ArrayList;

import de.fhg.iais.roberta.components.ActorType;
import de.fhg.iais.roberta.components.SensorType;
import de.fhg.iais.roberta.components.UsedActor;
import de.fhg.iais.roberta.components.UsedSensor;
import de.fhg.iais.roberta.components.arduino.MbotConfiguration;
import de.fhg.iais.roberta.syntax.Phrase;
import de.fhg.iais.roberta.syntax.action.motor.CurveAction;
import de.fhg.iais.roberta.syntax.action.motor.DriveAction;
import de.fhg.iais.roberta.syntax.action.motor.TurnAction;
import de.fhg.iais.roberta.syntax.action.sound.PlayNoteAction;
import de.fhg.iais.roberta.syntax.action.sound.ToneAction;
import de.fhg.iais.roberta.syntax.actors.arduino.mbot.DisplayImageAction;
import de.fhg.iais.roberta.syntax.actors.arduino.mbot.DisplayTextAction;
import de.fhg.iais.roberta.syntax.actors.arduino.mbot.ExternalLedOffAction;
import de.fhg.iais.roberta.syntax.actors.arduino.mbot.ExternalLedOnAction;
import de.fhg.iais.roberta.syntax.actors.arduino.mbot.LedOffAction;
import de.fhg.iais.roberta.syntax.actors.arduino.mbot.LedOnAction;
import de.fhg.iais.roberta.syntax.check.hardware.RobotUsedHardwareCollectorVisitor;
import de.fhg.iais.roberta.syntax.expressions.arduino.LedMatrix;
import de.fhg.iais.roberta.syntax.expressions.arduino.RgbColor;
import de.fhg.iais.roberta.syntax.sensor.generic.AccelerometerSensor;
import de.fhg.iais.roberta.syntax.sensor.generic.GyroSensor;
import de.fhg.iais.roberta.syntax.sensor.generic.LightSensor;
import de.fhg.iais.roberta.syntax.sensor.generic.TemperatureSensor;
import de.fhg.iais.roberta.syntax.sensor.generic.VoltageSensor;
import de.fhg.iais.roberta.syntax.sensors.arduino.mbot.AmbientLightSensor;
import de.fhg.iais.roberta.syntax.sensors.arduino.mbot.FlameSensor;
import de.fhg.iais.roberta.syntax.sensors.arduino.mbot.GetSampleSensor;
import de.fhg.iais.roberta.syntax.sensors.arduino.mbot.Joystick;
import de.fhg.iais.roberta.syntax.sensors.arduino.mbot.PIRMotionSensor;
import de.fhg.iais.roberta.visitors.arduino.MbotAstVisitor;

/**
 * This visitor collects information for used actors and sensors in blockly program.
 *
 * @author kcvejoski
 */
public class UsedHardwareCollectorVisitor extends RobotUsedHardwareCollectorVisitor implements MbotAstVisitor<Void> {
    public UsedHardwareCollectorVisitor(ArrayList<ArrayList<Phrase<Void>>> phrasesSet, MbotConfiguration configuration) {
        super(configuration);
        check(phrasesSet);
    }

    @Override
    public Void visitTemperatureSensor(TemperatureSensor<Void> temperatureSensor) {
        usedSensors.add(new UsedSensor(temperatureSensor.getPort(), SensorType.TEMPERATURE, null));
        return null;
    }

    @Override
    public Void visitJoystick(Joystick<Void> joystick) {
        usedSensors.add(new UsedSensor(joystick.getPort(), SensorType.JOYSTICK, null));
        return null;
    }

    @Override
    public Void visitAmbientLightSensor(AmbientLightSensor<Void> lightSensor) {
        usedSensors.add(new UsedSensor(lightSensor.getPort(), SensorType.AMBIENT_LIGHT, null));
        return null;
    }

    @Override
    public Void visitLightSensor(LightSensor<Void> lightSensor) {
        usedSensors.add(new UsedSensor(lightSensor.getPort(), SensorType.LINE_FOLLOWER, lightSensor.getMode()));
        return null;
    }

    @Override
    public Void visitAccelerometerSensor(AccelerometerSensor<Void> accelerometer) {
        usedSensors.add(new UsedSensor(accelerometer.getPort(), SensorType.ACCELEROMETER, accelerometer.getMode()));
        return null;
    }

    @Override
    public Void visitGyroSensor(GyroSensor<Void> gyroSensor) {
        usedSensors.add(new UsedSensor(gyroSensor.getPort(), SensorType.GYRO, gyroSensor.getMode()));
        return null;
    }

    @Override
    public Void visitFlameSensor(FlameSensor<Void> flameSensor) {
        usedSensors.add(new UsedSensor(flameSensor.getPort(), SensorType.FLAMESENSOR, null));
        return null;
    }

    @Override
    public Void visitPIRMotionSensor(PIRMotionSensor<Void> motionSensor) {
        usedSensors.add(new UsedSensor(motionSensor.getPort(), SensorType.PIR_MOTION, null));
        return null;
    }

    @Override
    public Void visitToneAction(ToneAction<Void> toneAction) {
        super.visitToneAction(toneAction);
        usedActors.add(new UsedActor(null, ActorType.BUZZER));
        return null;
    }

    @Override
    public Void visitPlayNoteAction(PlayNoteAction<Void> playNoteAction) {
        super.visitPlayNoteAction(playNoteAction);
        usedActors.add(new UsedActor(null, ActorType.BUZZER));
        return null;
    }

    @Override
    public Void visitLedOnAction(LedOnAction<Void> ledOnAction) {
        usedActors.add(new UsedActor(null, ActorType.LED_ON_BOARD));
        return null;
    }

    @Override
    public Void visitLedOffAction(LedOffAction<Void> ledOffAction) {
        usedActors.add(new UsedActor(null, ActorType.LED_ON_BOARD));
        return null;
    }

    @Override
    public Void visitExternalLedOnAction(ExternalLedOnAction<Void> externalLedOnAction) {
        usedActors.add(new UsedActor(externalLedOnAction.getPort(), ActorType.EXTERNAL_LED));
        return null;
    }

    @Override
    public Void visitExternalLedOffAction(ExternalLedOffAction<Void> externalLedOffAction) {
        usedActors.add(new UsedActor(externalLedOffAction.getPort(), ActorType.EXTERNAL_LED));
        return null;
    }

    @Override
    public Void visitDriveAction(DriveAction<Void> driveAction) {
        driveAction.getParam().getSpeed().visit(this);
        if ( driveAction.getParam().getDuration() != null ) {
            driveAction.getParam().getDuration().getValue().visit(this);
        }
        if ( brickConfiguration != null ) {
            usedActors.add(new UsedActor(brickConfiguration.getLeftMotorPort(), ActorType.DIFFERENTIAL_DRIVE));
        }
        return null;
    }

    @Override
    public Void visitCurveAction(CurveAction<Void> curveAction) {
        curveAction.getParamLeft().getSpeed().visit(this);
        curveAction.getParamRight().getSpeed().visit(this);
        if ( curveAction.getParamLeft().getDuration() != null ) {
            curveAction.getParamLeft().getDuration().getValue().visit(this);
        }
        if ( brickConfiguration != null ) {
            usedActors.add(new UsedActor(brickConfiguration.getLeftMotorPort(), ActorType.DIFFERENTIAL_DRIVE));
        }
        return null;
    }

    @Override
    public Void visitTurnAction(TurnAction<Void> turnAction) {
        turnAction.getParam().getSpeed().visit(this);
        if ( turnAction.getParam().getDuration() != null ) {
            turnAction.getParam().getDuration().getValue().visit(this);
        }
        if ( brickConfiguration != null ) {
            usedActors.add(new UsedActor(brickConfiguration.getLeftMotorPort(), ActorType.DIFFERENTIAL_DRIVE));
        }
        return null;
    }

    @Override
    public Void visitRgbColor(RgbColor<Void> rgbColor) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Void visitImage(LedMatrix<Void> ledMatrix) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Void visitDisplayImageAction(DisplayImageAction<Void> displayImageAction) {
        usedActors.add(new UsedActor(displayImageAction.getPort(), ActorType.LED_MATRIX));
        return null;
    }

    @Override
    public Void visitDisplayTextAction(DisplayTextAction<Void> displayTextAction) {
        usedActors.add(new UsedActor(displayTextAction.getPort(), ActorType.LED_MATRIX));
        return null;
    }

    @Override
    public Void visitVoltageSensor(VoltageSensor<Void> voltageSensor) {
        usedSensors.add(new UsedSensor(voltageSensor.getPort(), SensorType.VOLTAGE, null));
        return null;
    }

    @Override
    public Void visitMbotGetSampleSensor(GetSampleSensor<Void> getSampleSensor) {
        // TODO Auto-generated method stub
        return null;
    }

}
