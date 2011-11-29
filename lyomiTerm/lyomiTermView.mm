//
//  lyomiTermView.mm
//  lyomiTerm
//
//

#import "lyomiTermView.h"
#import <AppKit/NSStringDrawing.h>

@implementation lyomiTermView

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
    // Drawing code here.
	font=[NSFont fontWithName:@"Courier" size:16.0f];
	defaultForeground=CGColorCreateGenericRGB(0.7, 0.7, 1.0, 1.0);
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
	
	NSGraphicsContext *nsGraphicsContext = [NSGraphicsContext currentContext];
	CGContextRef context = (CGContextRef)[nsGraphicsContext graphicsPort];
	
	// background rendering
	for (int i=0;i<TERMINAL_ROWS;i++) {
		for(int j=0;j<TERMINAL_COLS;j++) {
			int index=i*TERMINAL_COLS+j;
			int colorindex=pie.background[index];
			CGColorRef color=(colorindex==-2)?defaultBackground:(colorindex==-1)?defaultForeground:colors[colorindex];
			CGContextSetFillColorWithColor(context, color);
			CGRect tmprect=CGRectMake(j*8.f, i*20.0f, 8.0f, 20.0f);
			CGContextAddRect(context,tmprect);
			CGContextFillRect(context,tmprect);			
			
			
		}
	}
	
	// draw cursor
	CGContextSetFillColorWithColor(context, cursorColor);
	CGRect tmprect=CGRectMake(pie.currentCol*8.f, pie.currentRow*20.0f, 8.0f, 20.0f);
	CGContextAddRect(context,tmprect);
	CGContextFillRect(context,tmprect);	
	
	// text rendering
	for (int i=0;i<TERMINAL_ROWS;i++) {
		for(int j=0;j<TERMINAL_COLS;j++) {
			int index=i*TERMINAL_COLS+j;
			int colorindex=pie.foreground[index];
			CGColorRef color=(colorindex==-2)?defaultBackground:(colorindex==-1)?defaultForeground:colors[colorindex];
			CGContextSetFillColorWithColor(context, color);			
			NSString *str=[NSString stringWithFormat:@"%C", pie.screen[index]];
			[str drawAtPoint:NSMakePoint(j*8.0f,i*20.0f)];
		}
	}

}

@end
