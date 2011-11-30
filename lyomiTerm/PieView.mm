//
//  lyomiTermView.mm
//  lyomiTerm
//
//

#import "PieView.h"
#import "Korean.h"
#import <AppKit/NSStringDrawing.h>

static Korean korean;

@implementation PieView

@synthesize pie;

- (void)awakeFromNib 
{
	pie=[[PieConnection alloc] init];
	[pie connectToHost:@"pie.kaist.ac.kr" onPort:23]; //temporary
	pie.pieView=self;
	[self.window makeKeyWindow];
	[self.window makeFirstResponder:self];
}

- (void)drawRect:(NSRect)dirtyRect
{
	if(pie==nil) return;

	font=[NSFont fontWithName:@"Courier" size:16.0f];
	defaultForeground=CGColorCreateGenericRGB(0.8, 0.8, 0.8, 1.0);
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
	
	nsForeground=[NSColor colorWithDeviceRed:0.8 green:0.8 blue:0.8 alpha:1.0];
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
			CGRect tmprect=CGRectMake(j*8.0f, 460.0f-i*20.0f, 8.0f, 20.0f);
			CGContextAddRect(context,tmprect);
			CGContextFillRect(context,tmprect);			
			
			[nsGraphicsContext restoreGraphicsState];
		}
	}
	
	// draw cursor
	CGContextSetFillColorWithColor(context, cursorColor);
	CGRect tmprect=CGRectMake(pie.currentCol*8.f, 460.0f-pie.currentRow*20.0f, 8.0f, 20.0f);
	CGContextAddRect(context,tmprect);
	CGContextFillRect(context,tmprect);	
	
	// text rendering
	NSDictionary *attr=[NSMutableDictionary dictionary];
	NSFont *courier = [[NSFontManager sharedFontManager] fontWithFamily:@"Courier New" traits:0 weight:6 size:14.0];
	NSFont *nanum = [[NSFontManager sharedFontManager] fontWithFamily:@"NanumGothic" traits:0 weight:6 size:14.0];
	if(nanum == nil) nanum = courier;

	for (int i=0;i<TERMINAL_ROWS;i++) {
		for(int j=0;j<TERMINAL_COLS;j++) {
			[nsGraphicsContext saveGraphicsState];
			
			int index=i*TERMINAL_COLS+j;
			unichar c = pie.screen[index];
			if((c>=0x3130 && c<=0x318F) || (c>=0x1100 && c<=0x11FF) || (c>=0xAC00 && c<=0xD7AF))
				[attr setValue:nanum forKey:NSFontAttributeName];
			else
				[attr setValue:courier forKey:NSFontAttributeName];
			
			int colorindex=pie.foreground[index];
			NSColor *color=(colorindex==-2)?nsBackground:(colorindex==-1)?nsForeground:nsColors[colorindex];
			[attr setValue:color forKey:NSForegroundColorAttributeName];
			
			NSString *str=[NSString stringWithFormat:@"%C", c];
			[str drawAtPoint:CGPointMake(j*8.0f,460.0f-i*20.0f) withAttributes:attr];
			
			[nsGraphicsContext restoreGraphicsState];
		}
	}
	
	// temporary korean text at the cursor
	if(tempCharacter != 0) {
		CGContextSetFillColorWithColor(context, cursorColor);
		CGRect tmprect=CGRectMake((pie.currentCol+1)*8.f, 460.0f-pie.currentRow*20.0f, 8.0f, 20.0f);
		CGContextAddRect(context,tmprect);
		CGContextFillRect(context,tmprect);	
		
		[attr setValue:nanum forKey:NSFontAttributeName];
		[attr setValue:nsForeground forKey:NSForegroundColorAttributeName];
		NSString *str=[NSString stringWithFormat:@"%C", tempCharacter];
		[str drawAtPoint:CGPointMake(pie.currentCol*8.0f, 460.0f-pie.currentRow*20.0f) withAttributes:attr];
	}

}

- (BOOL)acceptsFirstResponder 
{ 
	return YES; 
}

- (void)keyDown:(NSEvent *)theEvent
{
	if(theEvent.keyCode == 51) {
		if(korean.getSize() != 0) {
			tempCharacter = korean.backspace();
			[self setNeedsDisplay:YES];
		} else {
			[self sendKey:8];
		}
	} else {
		int c=[theEvent.characters characterAtIndex:0];
		
		if((c>=0x3130 && c<=0x318F) || (c>=0x1100 && c<=0x11FF) || (c>=0xAC00 && c<=0xD7AF)) {	// korean letters
			unichar result = korean.add(c);
			tempCharacter = korean.value();
			[self setNeedsDisplay:YES];
			if(result) {
				[self sendString:[NSString stringWithFormat:@"%C", result]];
			}
		} else {
			unichar result = korean.clear();
			if(result) {
				[self sendString:[NSString stringWithFormat:@"%C", result]];
			}
			
			[self sendKey:c];
			tempCharacter = 0;
			[self setNeedsDisplay:YES];
		}	
	}
	
	[super keyDown:theEvent];
}

- (void)sendKey:(int)key {
	if(key=='\n') {
		[pie send:"\r\n"];
	} else if(key<0x80) {
		[pie send:(char *)&key length:1];
	} 
}

- (void)sendString:(NSString *)str {
	const char *cstr=[str cStringUsingEncoding:pie.encoding];
	[pie send:cstr length:(int)strlen(cstr)];
}

- (void)disconnected {
	[self.window setTitle:@"lyomiTerm - disconnected"];
}

@end

