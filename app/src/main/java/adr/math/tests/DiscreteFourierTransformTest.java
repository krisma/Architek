package adr.math.tests;

import junit.framework.TestCase;

import adr.math.DiscreteFourierTransform;
import adr.structures.ComplexNumber;

public class DiscreteFourierTransformTest extends TestCase
{
    public DiscreteFourierTransformTest(String name)
    {
	super(name);
    }

    public void testDft()
    {
	ComplexNumber[] data = new ComplexNumber[3];
	data[0] = new ComplexNumber(1, 0);
	data[1] = new ComplexNumber(2, 0);
	data[2] = new ComplexNumber(3, 0);
	ComplexNumber[] out = DiscreteFourierTransform.fft(data);
	System.out.println(out[0] + " " + out[1] + " " + out[2] + "\n");

	out = DiscreteFourierTransform.ifft(out);
	System.out.println(out[0] + " " + out[1] + " " + out[2] + "\n");
    }
}