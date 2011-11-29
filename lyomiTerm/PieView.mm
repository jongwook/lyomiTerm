//
//  lyomiTermView.mm
//  lyomiTerm
//
//

#import "PieView.h"
#import <AppKit/NSStringDrawing.h>

@implementation PieView

@synthesize pie;

- (id)initWithFrame:(NSRect)frame
{
    self = [super initWithFrame:frame];
    if (self) {
        // Initialization code here.
    }
    
    return self;
}

- (void)drawRect:(NSRect)dirtyRect
{
	NSLog(@"drawing...");
	
	if(pie==nil) {
		pie=[[PieConnection alloc] init];
		[pie connectToHost:@"loco.kaist.ac.kr" onPort:23];
		pie.pieView=self;
	}

	font=[NSFont fontWithName:@"Courier" size:16.0f];
	defaultForeground=CGColorCreateGenericRGB(0.7, 0.7, 0.7, 1.0);
	defaultBackground=CGColorCreateGenericRGB(0.2, 0.3, 0.5, 1.0);
	cursorColor=CGColorCreateGenericRGB(0.4, 1.0, 0.0, 1.0);
	
	colors[0]=CGColorCreateGenericRGB(0.0, 0.0, 0.0, 1.0);
	colors[1]=CGColorCreateGenericRGB(1.0, 0.0, 0.0, 1.0);
	colors[2]=CGColorCreateGenericRGB(0.0, 1.0, 0.0, 1.0);
	colors[3]=CGColorCreateGenericRGB(1.0, 1.0, 0.0, 1.0);
	colors[4]=CGColorCreateGenericRGB(0.0, 0.0, 1.0, 1.0);
	colors[5]=CGColorCreateGenericRGB(1.0, 0.0, 1.0, 1.0);
	colors[6]=CGColorCreateGenericRGB(0.0, 1.0, 1.0, 1.0);
	colors[7]=CGColorCreateGenericRGB(1.0, 1.0, 1.0, 1.0);
	
	nsForeground=[NSColor colorWithDeviceRed:0.7 green:0.7 blue:0.7 alpha:1.0];
	nsBackground=[NSColor colorWithDeviceRed:0.2 green:0.3 blue:0.5 alpha:1.0];
	
	nsColors[0]=[NSColor blackColor];
	nsColors[1]=[NSColor redColor];
	nsColors[2]=[NSColor greenColor];
	nsColors[3]=[NSColor yellowColor];
	nsColors[4]=[NSColor blueColor];
	nsColors[5]=[NSColor magentaColor];
	nsColors[6]=[NSColor cyanColor];
	nsColors[7]=[NSColor whiteColor];
	
	NSGraphicsContext *nsGraphicsContext = [NSGraphicsContext currentContext];
	CGContextRef context = (CGContextRef)[nsGraphicsContext graphicsPort];
	
	// background rendering
	for (int i=0;i<TERMINAL_ROWS;i++) {
		for(int j=0;j<TERMINAL_COLS;j++) {
			[nsGraphicsContext saveGraphicsState];
			
			int index=i*TERMINAL_COLS+j;
			int colorindex=pie.background[index];
			CGColorRef color=(colorindex==-2)?defaultBackground:(colorindex==-1)?defaultForeground:colors[colorindex];
			CGContextSetFillColorWithColor(context, color);
			CGRect tmprect=CGRectMake(j*8.f, i*20.0f, 8.0f, 20.0f);
			CGContextAddRect(context,tmprect);
			CGContextFillRect(context,tmprect);			
			
			[nsGraphicsContext restoreGraphicsState];
		}
	}
	
	// draw cursor
	CGContextSetFillColorWithColor(context, cursorColor);
	CGRect tmprect=CGRectMake(pie.currentCol*8.f, pie.currentRow*20.0f, 8.0f, 20.0f);
	CGContextAddRect(context,tmprect);
	CGContextFillRect(context,tmprect);	
	
	// text rendering
	NSDictionary *attr=[NSMutableDictionary dictionary];
						
	[attr setValue:[[NSFontManager sharedFontManager] fontWithFamily:@"Courier New" traits:0 weight:9 size:15.0]
			forKey:NSFontAttributeName];

	for (int i=0;i<TERMINAL_ROWS;i++) {
		for(int j=0;j<TERMINAL_COLS;j++) {
			[nsGraphicsContext saveGraphicsState];
			
			int index=i*TERMINAL_COLS+j;
			int colorindex=pie.foreground[index];
			NSColor *color=(colorindex==-2)?nsBackground:(colorindex==-1)?nsForeground:nsColors[colorindex];
			[attr setValue:color forKey:NSForegroundColorAttributeName];
			NSString *str=[NSString stringWithFormat:@"%C", pie.screen[index]];
			[str drawAtPoint:CGPointMake(j*8.0f,460.0f-i*20.0f) withAttributes:attr];
			
			[nsGraphicsContext restoreGraphicsState];
		}
	}

}

@end
