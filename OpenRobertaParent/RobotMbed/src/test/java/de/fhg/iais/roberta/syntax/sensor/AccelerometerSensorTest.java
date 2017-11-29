package de.fhg.iais.roberta.syntax.sensor;

import org.junit.Assert;
import org.junit.Test;

import de.fhg.iais.roberta.util.test.mbed.Helper;

public class AccelerometerSensorTest {
    Helper h = new Helper();

    @Test
    public void make_ByDefault_ReturnInstanceOfAccelerometerSensorClass() throws Exception {
        String expectedResult =
            "BlockAST [project=[[Location [x=88, y=63], "
                + "MainTask [], "
                + "DisplayTextAction [TEXT, SensorExpr [Accelerometer [port = NO_PORT, coordinate  = X]]], "
                + "DisplayTextAction [TEXT, SensorExpr [Accelerometer [port = NO_PORT, coordinate  = Y]]], "
                + "DisplayTextAction [TEXT, SensorExpr [Accelerometer [port = NO_PORT, coordinate  = Z]]], "
                + "DisplayTextAction [TEXT, SensorExpr [Accelerometer [port = NO_PORT, coordinate  = STRENGTH]]]]]]";

        String result = this.h.generateTransformerString("/sensor/acceleration_sensor.xml");

        Assert.assertEquals(expectedResult, result);
    }

    @Test
    public void astToBlock_XMLtoJAXBtoASTtoXML_ReturnsSameXML() throws Exception {
        this.h.assertTransformationIsOk("/sensor/acceleration_sensor.xml");
    }

}
